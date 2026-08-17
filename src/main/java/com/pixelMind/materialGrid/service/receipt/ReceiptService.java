package com.pixelMind.materialGrid.service.receipt;

import com.pixelMind.materialGrid.dto.response.receipt.ReceiptSummaryDTO;

import java.time.LocalDate;

public interface ReceiptService {

    ReceiptSummaryDTO getReceiptData(LocalDate date, String vehicleNumber);

    byte[] generateReceiptPdf(LocalDate date, String vehicleNumber);
}
