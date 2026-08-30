package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.DailyRouteCreateRequest;
import com.pixelMind.materialGrid.dto.request.DailyRouteUpdateRequest;
import com.pixelMind.materialGrid.dto.response.DailyRouteResponse;
import com.pixelMind.materialGrid.entity.DailyRoute;
import com.pixelMind.materialGrid.entity.PriceRate;
import com.pixelMind.materialGrid.entity.Route;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.DailyRouteMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.PriceRateRepository;
import com.pixelMind.materialGrid.repository.RouteRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.DailyRouteService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * priceRateId is never accepted from the client (see
 * DailyRouteCreateRequest/UpdateRequest) - both create and update resolve
 * the currently ACTIVE PriceRate internally and recompute amount from it,
 * so the client can never manipulate either. Update now also re-validates
 * vehicle/route (see architectural decision #11) rather than treating them
 * as immutable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRouteServiceImpl implements DailyRouteService {

    private final DailyRouteRepository dailyRouteRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final PriceRateRepository priceRateRepository;
    private final DailyRouteMapper dailyRouteMapper;

    @Override
    @Transactional
    public DailyRouteResponse createDailyRoute(DailyRouteCreateRequest request) {
        Vehicle vehicle = findVehicleOrThrow(request.getVehicleId());
        Route route = findRouteOrThrow(request.getRouteId());

        String actor = SecurityUtil.getCurrentUsername();
        BigDecimal amount = computeAmount(vehicle, route);

        DailyRoute dailyRoute = DailyRoute.builder()
                .date(request.getDate())
                .vehicle(vehicle)
                .route(route)
                .amount(amount)
                .billNumber(request.getBillNumber())
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        DailyRoute saved = dailyRouteRepository.save(dailyRoute);
        log.info("DailyRoute created: id={}, vehicleId={}, routeId={}, amount={}, by={}",
                saved.getId(), vehicle.getId(), route.getId(), amount, actor);
        return dailyRouteMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyRouteResponse getDailyRoute(Long id) {
        return dailyRouteMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DailyRouteResponse> search(
            LocalDate date,
            LocalDate createdDate,
            String billNumber,
            Long vehicleId,
            Long routeId,
            Long fileHistoryId,
            Pageable pageable
    ) {
        LocalDateTime createdDateFrom = createdDate != null ? createdDate.atStartOfDay() : null;
        LocalDateTime createdDateTo = createdDate != null ? createdDate.plusDays(1).atStartOfDay() : null;
        String cleanBillNumber = (billNumber != null && !billNumber.isBlank()) ? billNumber.trim() : null;

        return dailyRouteRepository.search(
                        date,
                        createdDateFrom,
                        createdDateTo,
                        cleanBillNumber,
                        vehicleId,
                        routeId,
                        fileHistoryId,
                        pageable)
                .map(dailyRouteMapper::toResponse);
    }

    @Override
    @Transactional
    public DailyRouteResponse updateDailyRoute(Long id, DailyRouteUpdateRequest request) {
        DailyRoute dailyRoute = findOrThrow(id);
        Vehicle vehicle = findVehicleOrThrow(request.getVehicleId());
//        Route route = findRouteOrThrow(request.getRouteId());

        dailyRoute.setDate(request.getDate());
        dailyRoute.setVehicle(vehicle);
//        dailyRoute.setRoute(route);
//        dailyRoute.setAmount(computeAmount(vehicle, route));
        dailyRoute.setBillNumber(request.getBillNumber());
        dailyRoute.setModifiedBy(SecurityUtil.getCurrentUsername());

        DailyRoute saved = dailyRouteRepository.save(dailyRoute);
        log.info("DailyRoute updated: id={}, by={}", saved.getId(), dailyRoute.getModifiedBy());
        return dailyRouteMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDailyRoute(Long id) {
        DailyRoute dailyRoute = findOrThrow(id);
        dailyRoute.setDeleted(true);
        dailyRoute.setModifiedBy(SecurityUtil.getCurrentUsername());
        dailyRouteRepository.save(dailyRoute);
        log.info("DailyRoute soft-deleted: id={}, by={}", id, dailyRoute.getModifiedBy());
    }

    private BigDecimal computeAmount(Vehicle vehicle, Route route) {
        return vehicle.getCapacity()
                .multiply(route.getPrice())
                .multiply(route.getKm())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PriceRate findActivePriceRateOrThrow() {
        return priceRateRepository.findByStatus(PriceRateStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "No active price rate is available.", ErrorCodeConstants.ACTIVE_PRICE_RATE_NOT_FOUND));
    }

    private Vehicle findVehicleOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + id, ErrorCodeConstants.VEHICLE_NOT_FOUND));
    }

    private Route findRouteOrThrow(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Route not found with id: " + id, ErrorCodeConstants.ROUTE_NOT_FOUND));
    }

    private DailyRoute findOrThrow(Long id) {
        return dailyRouteRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Daily route not found with id: " + id, ErrorCodeConstants.DAILY_ROUTE_NOT_FOUND));
    }
}