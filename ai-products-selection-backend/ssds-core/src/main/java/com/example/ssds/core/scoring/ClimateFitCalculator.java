package com.example.ssds.core.scoring;

import java.math.BigDecimal;

/**
 * `CLIMATE` 因子：適溫區間與歷史月均溫的適配度（規格書 §FR-17-2）。
 *
 * <pre>
 * Tmin ≤ T ≤ Tmax  → fit = 1.0
 * 否則              → fit = max(0, 1 − distance / tolerance)
 *      distance = (T &lt; Tmin) ? (Tmin − T) : (T − Tmax)
 * </pre>
 *
 * <p>短期天氣預報不得用於此計算（僅供開團時機提醒，AC-17-4）——輸入必須是
 * {@code climate_normal} 的歷史同期統計，呼叫端負責保證這一點。
 */
public final class ClimateFitCalculator {

    /** 預設容忍範圍 12°C，可由 SYS_ADMIN 調整（§FR-17-2）。 */
    public static final BigDecimal DEFAULT_TOLERANCE = BigDecimal.valueOf(12);

    private ClimateFitCalculator() {
    }

    public static BigDecimal fit(BigDecimal avgTemp, BigDecimal tMin, BigDecimal tMax, BigDecimal tolerance) {
        if (avgTemp.compareTo(tMin) >= 0 && avgTemp.compareTo(tMax) <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal distance = avgTemp.compareTo(tMin) < 0
                ? tMin.subtract(avgTemp)
                : avgTemp.subtract(tMax);
        BigDecimal fit = BigDecimal.ONE.subtract(ScoreMath.safeDivide(distance, tolerance));
        return ScoreMath.round(fit.max(BigDecimal.ZERO), 4);
    }
}
