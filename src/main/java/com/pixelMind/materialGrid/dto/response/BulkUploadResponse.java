package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BulkUploadResponse {
    private boolean success;
    private String message;
    private int totalRows;
    private int successCount;
    private int errorCount;
    private List<ExcelValidationError> errors;
}