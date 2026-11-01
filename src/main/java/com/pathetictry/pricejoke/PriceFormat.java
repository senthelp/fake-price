package com.pathetictry.pricejoke;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

final class PriceFormat {
    private static final String[] SUFFIX = {"", "K", "M", "B", "T"};

    private PriceFormat() {}

    /** Parses 5m, 250k, 1b, 1.5m, 1,000. Returns -1 if invalid. */
    static long parse(String input) {
        String s = input.trim().replace(",", "").toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return -1;
        BigDecimal mult = BigDecimal.ONE;
        int i = "kmbt".indexOf(s.charAt(s.length() - 1));
        if (i >= 0) {
            mult = BigDecimal.TEN.pow(3 * (i + 1));
            s = s.substring(0, s.length() - 1);
        }
        try {
            long v = new BigDecimal(s).multiply(mult).setScale(0, RoundingMode.DOWN).longValueExact();
            return v < 0 ? -1 : v;
        } catch (NumberFormatException | ArithmeticException e) {
            return -1;
        }
    }

    /** 22 -> "22", 5000000 -> "5M", 79200000 -> "79.2M". */
    static String format(long v) {
        double d = v;
        int i = 0;
        while (d >= 1000 && i < 4) {
            d /= 1000;
            i++;
        }
        if (i == 0) return Long.toString(v);
        String n = String.format(Locale.ROOT, "%.1f", Math.floor(d * 10) / 10);
        if (n.endsWith(".0")) n = n.substring(0, n.length() - 2);
        return n + SUFFIX[i];
    }
}
