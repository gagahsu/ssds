package com.example.ssds.core.scoring;

import java.math.BigDecimal;

/**
 * 熱度絕對量級門檻，非加權因子（規格書 §5.2.1-a）。
 *
 * <pre>
 * 若 heat_volume &lt; HEAT_VOLUME_FLOOR（該品類近 90 日中位數的 20%）
 *    → TREND 因子標為「量級不足」，normalized_value = 0，且不觸發 HEAT_SURGE 示警
 *    → 但不視為「無資料」，權重照常套用（此為刻意的低分，不是資料缺漏）
 * </pre>
 */
public final class HeatVolumeGate {

    private static final BigDecimal FLOOR_RATIO = BigDecimal.valueOf(0.2);

    private HeatVolumeGate() {
    }

    /** @param categoryMedian90d 該品類近 90 日熱度中位數 */
    public static BigDecimal floor(BigDecimal categoryMedian90d) {
        return categoryMedian90d.multiply(FLOOR_RATIO);
    }

    /** @return true 表示量級不足（低於門檻），TREND normalized_value 應強制為 0，但 data_available 仍為 true */
    public static boolean belowFloor(BigDecimal heatVolume, BigDecimal categoryMedian90d) {
        return heatVolume.compareTo(floor(categoryMedian90d)) < 0;
    }
}
