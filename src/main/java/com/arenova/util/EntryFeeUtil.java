package com.arenova.util;

public final class EntryFeeUtil {

    private EntryFeeUtil() {
    }

    /** Parses entry fee string like "Rs. 150" / "150" / "Free" into NPR amount (0 if free). */
    public static int parseEntryFeeNpr(String entry) {
        if (entry == null || entry.isBlank()) {
            return 0;
        }
        String trimmed = entry.trim();
        if (trimmed.toLowerCase().contains("free")) {
            return 0;
        }
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatAmount(int npr) {
        return npr + ".00";
    }

    /** Parses "500.00", "Rs. 500", or "1,500" into whole NPR (ignores paisa). */
    public static long parseNprAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return 0;
        }
        try {
            String cleaned = amount.trim().replace(",", "");
            int dot = cleaned.indexOf('.');
            String whole = dot >= 0 ? cleaned.substring(0, dot) : cleaned;
            whole = whole.replaceAll("[^0-9]", "");
            if (whole.isEmpty()) {
                return 0;
            }
            return Long.parseLong(whole);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
