package com.pixelMind.materialGrid.service.impl.receipt;

import com.pixelMind.materialGrid.dto.response.receipt.ReceiptItemDTO;
import com.pixelMind.materialGrid.dto.response.receipt.ReceiptSummaryDTO;
import com.pixelMind.materialGrid.entity.dailyRoutes.DailyRoute;
import com.pixelMind.materialGrid.exception.BaseException;
import com.pixelMind.materialGrid.repository.dailyRoutes.DailyRouteRepository;
import com.pixelMind.materialGrid.service.receipt.ReceiptService;
import com.pixelMind.materialGrid.util.ReceiptPdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final DailyRouteRepository dailyRouteRepository;

    @Override
    @Transactional(readOnly = true)
    public ReceiptSummaryDTO getReceiptData(LocalDate date, String vehicleNumber) {
        if (date == null) {
            throw new BaseException(400, "Date parameter is required.");
        }
        if (vehicleNumber == null || vehicleNumber.trim().isBlank()) {
            throw new BaseException(400, "Vehicle number parameter is required.");
        }

        String normalizedVehicleNumber = vehicleNumber.trim();
        List<DailyRoute> routes = dailyRouteRepository.findByDateAndVehicleNumber(date, normalizedVehicleNumber);

        if (routes == null || routes.isEmpty()) {
            throw new BaseException(404, "No daily route records found for vehicle '" + normalizedVehicleNumber + "' on date " + date);
        }

        List<ReceiptItemDTO> items = new ArrayList<>();
        double totalCubes = 0.0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalLicenseFee = BigDecimal.ZERO;
        String projectSite = null;

        int rowIdx = 1;
        for (DailyRoute route : routes) {
            double cube = route.getCube() != null ? route.getCube() : 0.0;
            BigDecimal price = route.getPrice() != null ? route.getPrice() : BigDecimal.ZERO;
            BigDecimal itemTotalAmount = BigDecimal.valueOf(cube).multiply(price);
            BigDecimal dailyExpenses = route.getDailyExpenses() != null ? route.getDailyExpenses() : BigDecimal.ZERO;
            BigDecimal licenseFee = BigDecimal.ZERO;
            BigDecimal balance = itemTotalAmount.subtract(dailyExpenses).subtract(licenseFee);

            if (projectSite == null && route.getLand() != null && route.getLand().getLandName() != null) {
                projectSite = route.getLand().getLandName();
            } else if (projectSite == null && route.getRoute() != null && route.getRoute().getStartLocation() != null) {
                projectSite = route.getRoute().getStartLocation();
            }

            ReceiptItemDTO item = ReceiptItemDTO.builder()
                    .no(rowIdx++)
                    .lorryNo(route.getVehicle() != null ? route.getVehicle().getVehicleNumber() : normalizedVehicleNumber)
                    .routeCode(route.getRoute() != null ? route.getRoute().getRouteCode() : null)
                    .billNumber(route.getBillNumber())
                    .cube(cube)
                    .price(price)
                    .totalAmount(itemTotalAmount)
                    .dailyExpenses(dailyExpenses)
                    .licenseFee(licenseFee)
                    .balance(balance)
                    .build();

            items.add(item);
            totalCubes += cube;
            totalAmount = totalAmount.add(itemTotalAmount);
            totalExpenses = totalExpenses.add(dailyExpenses);
            totalLicenseFee = totalLicenseFee.add(licenseFee);
        }

        BigDecimal totalBalance = totalAmount.subtract(totalExpenses).subtract(totalLicenseFee);

        return ReceiptSummaryDTO.builder()
                .date(date)
                .vehicleNumber(normalizedVehicleNumber)
                .projectSite(projectSite != null ? projectSite : "Warakapola")
                .items(items)
                .totalCubes(totalCubes)
                .totalAmount(totalAmount)
                .totalExpenses(totalExpenses)
                .totalLicenseFee(totalLicenseFee)
                .totalBalance(totalBalance)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(LocalDate date, String vehicleNumber) {
        ReceiptSummaryDTO receipt = getReceiptData(date, vehicleNumber);
        log.info("Generating PDF receipt for date={}, vehicleNumber={}, itemsCount={}", date, vehicleNumber, receipt.getItems().size());
        return ReceiptPdfGenerator.generateReceiptPdf(receipt);
    }
}
