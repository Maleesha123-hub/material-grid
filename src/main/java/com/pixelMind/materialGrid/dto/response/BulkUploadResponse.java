package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * ADDED: fileHistoryId / fileName / fileType - populated on a successful
 * upload (see the three import services), left null on validation failure
 * since no FileHistory row exists yet in that case. Deliberately extending
 * this existing DTO rather than introducing a separate FileUploadResponse
 * type, to avoid a second, incompatible upload-response shape.
 */
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

    private Long fileHistoryId;
    private String fileName;
    private String fileType;
}