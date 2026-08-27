package com.example.ssds.core.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 評分計算共用的 BigDecimal 工具（規格書 §5）。
 *
 * <p>內部運算保留高精度，只在對外輸出（subtotal／contribution／raw_value 顯示值）時才四捨五入，
 * 避免逐步四捨五入造成 §11.1 黃金案例的 ±0.01 誤差累積。
 */
public final class ScoreMath {

    private static final int INTERNAL_SCALE = 10;

    private ScoreMath() {
    }

    public static BigDecimal round(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    /** 除法，分母為 0 時回傳 null 而非拋例外（呼叫端須自行處理「無法計算」）。 */
    public static BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
