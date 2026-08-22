package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceipt;
import com.pixelMind.materialGrid.service.DailyRoutePdfService;
import com.pixelMind.materialGrid.service.DailyRouteReportService;
import com.pixelMind.materialGrid.util.PdfFileNameUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * MODIFIED: only the return type of the service call changed (now
 * DailyRoutePaymentReceipt) - endpoints, parameters, and response headers
 * are unchanged from the date-range version. Still deliberately thin.
 */
@Tag(name = "Daily Route Reports", description = "Read-only vehicle payment receipt (PDF) for a date range")
@RestController
@RequestMapping("/api/v1/daily-routes/report")
@RequiredArgsConstructor
public class DailyRouteReportController {

    private final DailyRouteReportService dailyRouteReportService;
    private final DailyRoutePdfService dailyRoutePdfService;

    @Operation(summary = "Preview the vehicle payment receipt PDF inline (startDate + endDate + vehicleId)")
    @GetMapping("/preview")
    public ResponseEntity<byte[]> preview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam Long vehicleId) {
        return buildPdfResponse(startDate, endDate, vehicleId, ContentDisposition.inline());
    }

    @Operation(summary = "Download the vehicle payment receipt PDF as an attachment (startDate + endDate + vehicleId)")
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam Long vehicleId) {
        return buildPdfResponse(startDate, endDate, vehicleId, ContentDisposition.attachment());
    }

    private ResponseEntity<byte[]> buildPdfResponse(LocalDate startDate, LocalDate endDate, Long vehicleId,
                                                    ContentDisposition.Builder dispositionBuilder) {
        DailyRoutePaymentReceipt receipt = dailyRouteReportService.generateReport(startDate, endDate, vehicleId);
        byte[] pdfBytes = dailyRoutePdfService.generatePdf(receipt);

        String fileName = PdfFileNameUtil.buildFileName(receipt.getVehicleNumber(), receipt.getStartDate(), receipt.getEndDate());
        ContentDisposition disposition = dispositionBuilder.filename(fileName).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}