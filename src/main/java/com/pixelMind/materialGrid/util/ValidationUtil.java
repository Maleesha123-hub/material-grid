package com.pixelMind.materialGrid.util;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
