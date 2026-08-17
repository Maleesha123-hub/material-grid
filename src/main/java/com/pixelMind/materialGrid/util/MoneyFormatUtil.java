package com.pixelMind.materialGrid.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Centralizes monetary formatting (e.g. "97,680.00") so every part of the
 * PDF uses exactly the same rules - see spec section 36.
 */
public final class MoneyFormatUtil {

    private MoneyFormatUtil() {
    }

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    });

    public static String format(BigDecimal value) {
        BigDecimal safe = value != null ? value : BigDecimal.ZERO;
        return FORMAT.get().format(safe);
    }
}