package com.pixelMind.materialGrid.controller.dailyRoute;

import com.pixelMind.materialGrid.dto.request.DailyRouteCreateRequest;
import com.pixelMind.materialGrid.dto.request.DailyRouteUpdateRequest;
import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.CommonResponseDTO;
import com.pixelMind.materialGrid.dto.response.DailyRouteResponse;
import com.pixelMind.materialGrid.dto.response.PageResponse;
import com.pixelMind.materialGrid.service.dailyRoutes.DailyRouteImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Tag(name = "Daily Routes", description = "Vehicle+Route+active-PriceRate billing records; amount and price rate are always server-resolved")
@RestController
@RequestMapping(value = "/api/material-grid/daily-routes")
@RequiredArgsConstructor
public class DailyRouteController {

    private final DailyRouteImportService dailyRouteImportService;

    /**
     * This method is allowed to bulk upload of daily routes
     *
     * @param file
     * @return
     * @author - maleeshasa
     */
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponseDTO> bulkUpload(@RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(dailyRouteImportService.bulkUpload(file));

    }

}