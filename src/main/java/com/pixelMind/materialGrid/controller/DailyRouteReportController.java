package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.response.CommonResponseDTO;
import com.pixelMind.materialGrid.dto.response.DailyRouteReportResponse;
import com.pixelMind.materialGrid.dto.response.ReceiptSummaryDTO;
import com.pixelMind.materialGrid.service.DailyRoutePdfService;
import com.pixelMind.materialGrid.service.DailyRouteReportService;
import com.pixelMind.materialGrid.util.PdfFileNameUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Deliberately thin: request in, delegate to DailyRouteReportService for
 * data + calculation, delegate to DailyRoutePdfService for rendering,
 * attach the right Content-Disposition, return bytes. No business logic,
 * no database access, no PDF-library code here.
 */
@Tag(name = "Daily Route Reports", description = "Read-only PDF report for a single vehicle's Daily Route on a given date")
@RestController
@Slf4j
@RequestMapping("/api/v1/daily-routes/report")
@RequiredArgsConstructor
public class DailyRouteReportController {

    private final DailyRouteReportService dailyRouteReportService;
    private final DailyRoutePdfService dailyRoutePdfService;

    @GetMapping(value = "/summary")
    public ResponseEntity<CommonResponseDTO> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long vehicleId
    ) {

        log.info("DailyRouteReportController.getSummary() => accessed");

        return ResponseEntity.ok(
                new CommonResponseDTO(
                        dailyRouteReportService.getSummary(
                                date, vehicleId
                        ),
                        "Receipt summary",
                        HttpStatus.OK
                )
        );

    }

    @Operation(summary = "Preview the Daily Route report PDF inline (date + vehicleId)")
    @GetMapping("/preview")
    public ResponseEntity<byte[]> preview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long vehicleId) {

        log.info("DailyRouteReportController.preview() => accessed");

        return buildPdfResponse(date, vehicleId, ContentDisposition.inline());
    }

    @Operation(summary = "Download the Daily Route report PDF as an attachment (date + vehicleId)")
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long vehicleId) {

        log.info("DailyRouteReportController.download() => accessed");

        return buildPdfResponse(date, vehicleId, ContentDisposition.attachment());
    }

    private ResponseEntity<byte[]> buildPdfResponse(LocalDate date, Long vehicleId,
                                                    ContentDisposition.Builder dispositionBuilder) {

        log.info("DailyRouteReportController.buildPdfResponse() => accessed");

        DailyRouteReportResponse report = dailyRouteReportService.generateReport(date, vehicleId);

        byte[] pdfBytes = dailyRoutePdfService.generatePdf(report);

        String fileName = PdfFileNameUtil.buildFileName(report.getVehicleNumber(), report.getDate());
        ContentDisposition disposition = dispositionBuilder.filename(fileName).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}