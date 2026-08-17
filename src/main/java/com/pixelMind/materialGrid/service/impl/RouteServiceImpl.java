package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.CodeSequenceConstants;
import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.RouteCreateRequest;
import com.pixelMind.materialGrid.dto.request.RouteUpdateRequest;
import com.pixelMind.materialGrid.dto.response.RouteResponse;
import com.pixelMind.materialGrid.entity.Route;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.RouteMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.RouteRepository;
import com.pixelMind.materialGrid.service.RouteService;
import com.pixelMind.materialGrid.util.CodeGeneratorService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final DailyRouteRepository dailyRouteRepository;
    private final RouteMapper routeMapper;
    private final CodeGeneratorService codeGeneratorService;

    @Override
    @Transactional
    public RouteResponse createRoute(RouteCreateRequest request) {
        String actor = SecurityUtil.getCurrentUsername();

        // Code is reserved via its own short REQUIRES_NEW transaction (see
        // CodeGeneratorService) before this Route row is even built - so the
        // number is guaranteed unique the instant it's handed out, no matter
        // what happens to the rest of this transaction.
        String routeCode = codeGeneratorService.nextCode(
                CodeSequenceConstants.ROUTE_CODE_SEQUENCE,
                CodeSequenceConstants.ROUTE_CODE_PREFIX,
                CodeSequenceConstants.ROUTE_CODE_PAD_LENGTH);

        Route route = Route.builder()
                .routeCode(routeCode)
                .startLocation(request.getStartLocation())
                .endLocation(request.getEndLocation())
                .km(request.getKm())
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        Route saved = routeRepository.save(route);
        log.info("Route created: id={}, routeCode={}, by={}", saved.getId(), saved.getRouteCode(), actor);
        return routeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long id) {
        return routeMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RouteResponse> getRoutes(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return routeRepository
                    .findByStartLocationContainingIgnoreCaseOrEndLocationContainingIgnoreCase(search, search, pageable)
                    .map(routeMapper::toResponse);
        }
        return routeRepository.findAll(pageable).map(routeMapper::toResponse);
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(Long id, RouteUpdateRequest request) {
        Route route = findOrThrow(id);
        route.setStartLocation(request.getStartLocation());
        route.setEndLocation(request.getEndLocation());
        route.setKm(request.getKm());
        route.setModifiedBy(SecurityUtil.getCurrentUsername());
        // routeCode is never touched here - immutable by design (see
        // RouteUpdateRequest).

        Route saved = routeRepository.save(route);
        log.info("Route updated: id={}, by={}", saved.getId(), route.getModifiedBy());
        return routeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        Route route = findOrThrow(id);
        if (dailyRouteRepository.existsByRouteId(id)) {
            throw new BusinessException(
                    "Cannot delete route with existing daily route records. "
                            + "Daily routes are historical records and this route must be preserved for referential integrity.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }
        routeRepository.delete(route);
        log.info("Route deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    private Route findOrThrow(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Route not found with id: " + id, ErrorCodeConstants.ROUTE_NOT_FOUND));
    }
}
