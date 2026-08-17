package com.pixelMind.materialGrid.controller.receipt;

import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.receipt.ReceiptSummaryDTO;
import com.pixelMind.materialGrid.service.receipt.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Receipts", description = "PDF Receipt Generation and Preview for Daily Routes")
@RestController
@RequestMapping(value = {"/api/material-grid/receipts", "/api/v1/receipts"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReceiptController {

    private final ReceiptService receiptService;

    @Operation(summary = "Get receipt preview data as JSON for a particular date and vehicle number")
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<ReceiptSummaryDTO>> getReceiptPreview(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("vehicleNumber") String vehicleNumber) {

        ReceiptSummaryDTO receipt = receiptService.getReceiptData(date, vehicleNumber);
        return ResponseEntity.ok(ApiResponse.success("Receipt preview retrieved successfully", receipt));
    }

    @Operation(summary = "Generate and stream PDF receipt for a particular date and vehicle number")
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generateReceiptPdf(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("vehicleNumber") String vehicleNumber) {

        byte[] pdfBytes = receiptService.generateReceiptPdf(date, vehicleNumber);
        String filename = "Receipt_" + vehicleNumber.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + date + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @Operation(summary = "Download PDF receipt for a particular date and vehicle number")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("vehicleNumber") String vehicleNumber) {

        byte[] pdfBytes = receiptService.generateReceiptPdf(date, vehicleNumber);
        String filename = "Receipt_" + vehicleNumber.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + date + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }
}
