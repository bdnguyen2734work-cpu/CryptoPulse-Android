package com.cryptopulse.app.utils;

import java.text.DecimalFormat;
import java.util.Locale;

public class FormatUtils {
    private static final DecimalFormat PRICE_FMT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PCT_FMT   = new DecimalFormat("+0.00;-0.00");
    private static final DecimalFormat LARGE_FMT = new DecimalFormat("#,##0.##");

    public static String formatPrice(double price) {
        if (price >= 1000) return "$" + PRICE_FMT.format(price);
        if (price >= 1)    return "$" + new DecimalFormat("0.0000").format(price);
        return "$" + new DecimalFormat("0.00000000").format(price);
    }

    public static String formatPriceShort(double price) {
        return "$" + PRICE_FMT.format(price);
    }

    public static String formatPercent(double pct) {
        return PCT_FMT.format(pct) + "%";
    }

    public static String formatVolume(double vol) {
        if (vol >= 1_000_000_000) return String.format(Locale.US, "%.2fB", vol / 1_000_000_000);
        if (vol >= 1_000_000)     return String.format(Locale.US, "%.2fM", vol / 1_000_000);
        if (vol >= 1_000)         return String.format(Locale.US, "%.2fK", vol / 1_000);
        return LARGE_FMT.format(vol);
    }

    public static String formatPortfolio(double value) {
        return "$" + PRICE_FMT.format(value);
    }

    public static boolean isPositive(double val) { return val >= 0; }

    public static String price(double price) { return formatPrice(price); }
    public static String pct(double pct)     { return formatPercent(pct); }
    public static String volume(double vol) { return formatVolume(vol); }
    public static String shortPrice(double price) { return formatPriceShort(price); }
}