package com.pixelMind.materialGrid.service.impl.dailyRoutes;

import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.dailyRoutes.DailyRoutesExcelDataDTO;
import com.pixelMind.materialGrid.dto.response.CommonResponseDTO;
import com.pixelMind.materialGrid.entity.Land;
import com.pixelMind.materialGrid.entity.Route;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.dailyRoutes.DailyRoute;
import com.pixelMind.materialGrid.exception.BaseException;
import com.pixelMind.materialGrid.repository.LandRepository;
import com.pixelMind.materialGrid.repository.RouteRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.repository.dailyRoutes.DailyRouteRepository;
import com.pixelMind.materialGrid.service.dailyRoutes.DailyRouteImportService;
import com.pixelMind.materialGrid.util.ExcelUtil;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRouteImportServiceImpl implements DailyRouteImportService {

    private final DailyRouteRepository dailyRouteRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final LandRepository landRepository;

    @Override
    @Transactional
    public CommonResponseDTO bulkUpload(MultipartFile file) {

        Workbook workbook = ExcelUtil.openWorkbook(file);

        try {
            Sheet sheet = ExcelUtil.firstSheet(workbook);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);
            ExcelUtil.requireHeaders(headerIndex, ExcelConstants.DAILY_ROUTE_HEADERS);

            int dateCol = ExcelUtil.columnOf(headerIndex, "Date");
            int routeCol = ExcelUtil.columnOf(headerIndex, "Route_Code");
            int billCol = ExcelUtil.columnOf(headerIndex, "Bill_Number");
            int cubeCol = ExcelUtil.columnOf(headerIndex, "Cube");
            int kmCol = ExcelUtil.columnOf(headerIndex, "KM");
            int dailyExpensesCol = ExcelUtil.columnOf(headerIndex, "Daily_Expenses");
            int vehicleCol = ExcelUtil.columnOf(headerIndex, "Vehicle_Number");
            int landCol =  ExcelUtil.columnOf(headerIndex, "Land_Code");

            List<DailyRoutesExcelDataDTO> dailyRoutesExcelData = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelUtil.isRowEmpty(row)) {
                    continue;
                }

                int rowNumber = r + 1;

                // Date: not blank && valid date format (e.g. 17/08/2026)
                String dateRaw = ExcelUtil.readString(row, dateCol);
                if (dateRaw.isBlank()) {
                    throw new BaseException(400, "Row " + rowNumber + ": Date is required and cannot be blank.");
                }
                Optional<LocalDate> dateOpt = ExcelUtil.readDate(row, dateCol);
                if (dateOpt.isEmpty()) {
                    throw new BaseException(400, "Row " + rowNumber + ": Invalid Date format '" + dateRaw + "'. Expected format: dd/MM/yyyy.");
                }

                // Route_Code: not blank
                String routeCode = ExcelUtil.readString(row, routeCol);
                if (routeCode.isBlank()) {
                    throw new BaseException(400, "Row " + rowNumber + ": Route_Code is required and cannot be blank.");
                }

                // Bill_Number: not blank
                String billNumber = ExcelUtil.readString(row, billCol);
                if (billNumber.isBlank()) {
                    throw new BaseException(400, "Row " + rowNumber + ": Bill_Number is required and cannot be blank.");
                }

                // Cube: not blank and double
                String cubeRaw = ExcelUtil.readString(row, cubeCol);
                if (cubeRaw.isBlank()) {
                    throw new BaseException(400, "Row " + rowNumber + ": Cube is required and cannot be blank.");
                }
                Optional<Double> cubeOpt = ExcelUtil.readDouble(row, cubeCol);
                if (cubeOpt.isEmpty()) {
                    throw new BaseException(400, "Row " + rowNumber + ": Cube must be a valid double value, got: '" + cubeRaw + "'.");
                }

                // KM: not blank and double
                String kmRaw = ExcelUtil.readString(row, kmCol);
                if (kmRaw.isBlank()) {
                    throw new BaseException(400, "Row " + rowNumber + ": KM is required and cannot be blank.");
                }
                Optional<Double> kmOpt = ExcelUtil.readDouble(row, kmCol);
                if (kmOpt.isEmpty()) {
                    throw new BaseException(400, "Row " + rowNumber + ": KM must be a valid double value, got: '" + kmRaw + "'.");
                }

                // Daily_Expenses: not blank and BigDecimal/numeric
                String dailyExpensesRaw = ExcelUtil.readString(row, dailyExpensesCol);
                if (dailyExpensesRaw.isBlank()) {
                    throw new BaseException(400, "Row " + rowNumber + ": Daily_Expenses is required and cannot be blank.");
                }
                Optional<BigDecimal> dailyExpensesOpt = ExcelUtil.readBigDecimal(row, dailyExpensesCol);
                if (dailyExpensesOpt.isEmpty()) {
                    throw new BaseException(400, "Row " + rowNumber + ": Daily_Expenses must be a valid numeric value, got: '" + dailyExpensesRaw + "'.");
                }

                DailyRoutesExcelDataDTO rowData = new DailyRoutesExcelDataDTO();
                rowData.setRowNumber(rowNumber);
                rowData.setDate(dateOpt.get());
                rowData.setRouteCode(routeCode);
                rowData.setBillNumber(billNumber);
                rowData.setCube(cubeOpt.get());
                rowData.setKm(kmOpt.get());
                rowData.setDailyExpenses(dailyExpensesOpt.get());
                rowData.setVehicleNumber(ExcelUtil.readString(row, vehicleCol));
                rowData.setLandCode(ExcelUtil.readString(row, landCol));

                dailyRoutesExcelData.add(rowData);
            }

            if (dailyRoutesExcelData.isEmpty()) {
                throw new BaseException(400, "The uploaded file contains no data rows");
            }

            // Bulk fetch Routes by Route_Code
            Set<String> routeCodes = dailyRoutesExcelData.stream()
                    .map(DailyRoutesExcelDataDTO::getRouteCode)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toSet());
            Map<String, Route> routeByCode = routeRepository.findByRouteCodeIn(routeCodes).stream()
                    .collect(Collectors.toMap(Route::getRouteCode, r -> r));

            // Bulk fetch Vehicles by Vehicle_Number
            Set<String> vehicleNumbers = dailyRoutesExcelData.stream()
                    .map(DailyRoutesExcelDataDTO::getVehicleNumber)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toSet());
            Map<String, Vehicle> vehicleByNumber = vehicleNumbers.isEmpty() ? Map.of() :
                    vehicleRepository.findByVehicleNumberIn(vehicleNumbers).stream()
                            .collect(Collectors.toMap(Vehicle::getVehicleNumber, v -> v));

            // Bulk fetch Lands by Land_Code
            Set<String> landCodes = dailyRoutesExcelData.stream()
                    .map(DailyRoutesExcelDataDTO::getLandCode)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toSet());
            Map<String, Land> landByCode = landCodes.isEmpty() ? Map.of() :
                    landRepository.findByLandCodeIn(landCodes).stream()
                            .collect(Collectors.toMap(Land::getLandCode, l -> l));

            String actor = SecurityUtil.getCurrentUsername();

            List<DailyRoute> entities = dailyRoutesExcelData.stream()
                    .map(dto -> {
                        // 1. Validate and resolve Route
                        Route route = routeByCode.get(dto.getRouteCode());
                        if (route == null) {
                            throw new BaseException(400, "Row " + dto.getRowNumber() + ": Route code '" + dto.getRouteCode() + "' does not exist.");
                        }

                        // 2. Validate and resolve Vehicle if provided
                        Vehicle vehicle = null;
                        if (dto.getVehicleNumber() != null && !dto.getVehicleNumber().isBlank()) {
                            vehicle = vehicleByNumber.get(dto.getVehicleNumber());
                            if (vehicle == null) {
                                throw new BaseException(400, "Row " + dto.getRowNumber() + ": Vehicle number '" + dto.getVehicleNumber() + "' does not exist.");
                            }
                        }

                        // 3. Validate and resolve Land if provided
                        Land land = null;
                        if (dto.getLandCode() != null && !dto.getLandCode().isBlank()) {
                            land = landByCode.get(dto.getLandCode());
                            if (land == null) {
                                throw new BaseException(400, "Row " + dto.getRowNumber() + ": Land code '" + dto.getLandCode() + "' does not exist.");
                            }
                        }

                        double routeKm = route.getKm();

                        return (DailyRoute) DailyRoute.builder()
                                .date(dto.getDate())
                                .route(route)
                                .vehicle(vehicle)
                                .land(land)
                                .billNumber(dto.getBillNumber())
                                .cube(dto.getCube())
                                .km(routeKm)
                                .price(route.getPrice())
                                .dailyExpenses(dto.getDailyExpenses())
                                .active(true)
                                .createdBy(actor)
                                .modifiedBy(actor)
                                .modifiedDate(LocalDateTime.now())
                                .createdDate(LocalDateTime.now())
                                .build();
                    })
                    .toList();

            dailyRouteRepository.saveAll(entities);
            log.info("Bulk daily route upload successful: {} records saved, by={}", entities.size(), actor);

            return new CommonResponseDTO("Daily routes uploaded successfully", null, HttpStatus.OK);

        } finally {

            try {
                workbook.close();
            } catch (IOException ignored) {
            }

        }

    }

}