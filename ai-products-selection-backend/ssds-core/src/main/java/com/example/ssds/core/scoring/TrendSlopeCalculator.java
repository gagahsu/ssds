package com.example.ssds.core.scoring;

import java.math.BigDecimal;

/**
 * `TREND` 因子的雙窗口斜率（規格書 §5.3.3）。
 *
 * <pre>
 * slope_7d  = (heat_t − heat_{t-7})  / max(heat_{t-7}, ε)      ε = 1.0
 * slope_30d = (heat_t − heat_{t-30}) / max(heat_{t-30}, ε)
 * trend_raw = 0.7 × slope_7d + 0.3 × slope_30d
 * </pre>
 *
 * <p>不滿 7 日歷史時整個 TREND 因子標為無資料（呼叫端負責，本類別只算數學）；
 * 不滿 30 日時 {@code slope30d} 以現有最長區間計算，{@code shortHistory} 標示之。
 */
public final class TrendSlopeCalculator {

    private static final BigDecimal EPSILON = BigDecimal.ONE;
    private static final BigDecimal W7 = BigDecimal.valueOf(0.7);
    private static final BigDecimal W30 = BigDecimal.valueOf(0.3);

    private TrendSlopeCalculator() {
    }

    /**
     * @param heatAtLongestWindow 30 日前的合成熱度；歷史不滿 30 日時傳入現有最長區間的值
     * @param shortHistory        歷史是否不滿 30 日
     */
    public static TrendSlopeResult compute(
            BigDecimal heatToday, BigDecimal heatAt7dAgo, BigDecimal heatAtLongestWindow,
            boolean shortHistory) {
        BigDecimal slope7d = slope(heatToday, heatAt7dAgo);
        BigDecimal slope30d = slope(heatToday, heatAtLongestWindow);
        BigDecimal trendRaw = ScoreMath.round(
                W7.multiply(slope7d).add(W30.multiply(slope30d)), 4);
        boolean divergence = slope7d.signum() < 0 && slope30d.signum() > 0;
        return new TrendSlopeResult(
                ScoreMath.round(slope7d, 4), ScoreMath.round(slope30d, 4), trendRaw,
                divergence, shortHistory);
    }

    private static BigDecimal slope(BigDecimal heatT, BigDecimal heatTMinusN) {
        BigDecimal denominator = heatTMinusN.max(EPSILON);
        return ScoreMath.safeDivide(heatT.subtract(heatTMinusN), denominator);
    }
}
