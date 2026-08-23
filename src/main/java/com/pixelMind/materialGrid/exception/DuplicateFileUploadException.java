package com.pixelMind.materialGrid.exception;

/**
 * Subclass of the existing DuplicateResourceException (not an independent
 * exception type) so GlobalExceptionHandler's existing
 * @ExceptionHandler(DuplicateResourceException.class) already catches this
 * - Spring's handler resolution matches subclasses of the declared type -
 * meaning zero changes were needed in GlobalExceptionHandler for this
 * feature. This still gives a distinct, semantically clear exception name
 * at the throw site, which is what the "reuse the existing exception
 * architecture, don't duplicate it" instruction is really asking for.
 */
public class DuplicateFileUploadException extends DuplicateResourceException {

    public DuplicateFileUploadException(String message, String errorCode) {
        super(message, errorCode);
    }
}