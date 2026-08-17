package com.pixelMind.materialGrid.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String errorCode;

    public DuplicateResourceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
