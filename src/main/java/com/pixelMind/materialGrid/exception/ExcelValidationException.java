package com.pixelMind.materialGrid.exception;

import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import lombok.Getter;

import java.util.List;

/**
 * Carries the FULL set of row-level errors found while validating an
 * uploaded Excel file (never just the first one), plus the total row count,
 * so GlobalExceptionHandler can build a complete BulkUploadResponse without
 * re-deriving it. Thrown only before any database write happens for that
 * upload - see the @Transactional import-service methods.
 */
@Getter
public class ExcelValidationException extends RuntimeException {

    private final List<ExcelValidationError> errors;
    private final int totalRows;

    public ExcelValidationException(String message, List<ExcelValidationError> errors, int totalRows) {
        super(message);
        this.errors = errors;
        this.totalRows = totalRows;
    }
}