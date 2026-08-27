package com.example.ssds.core.scoring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * `FESTIVAL` 因子：時間窗函數，取多節慶最大值（規格書 §FR-17-1）。
 *
 * <pre>
 * 節慶因子(品項, 日期) = max over 所有節慶 f 之 [ affinity(品項, f) × window(日期, f, L) ]
 *
 * window(d, L) = 0.0    當  d &gt; L + 30
 *              = 1.0    當  L ≤ d ≤ L + 30
 *              = 0.5    當  0 ≤ d &lt; L
 *              = 0.0    當  d &lt; 0
 * </pre>
 *
 * 其中 {@code d = 節慶日 − 評估日}（正值代表節慶尚未到），{@code L} 為品類前置天數。
 */
public final class FestivalWindowCalculator {

    private static final BigDecimal HALF = BigDecimal.valueOf(0.5);

    private FestivalWindowCalculator() {
    }

    public static BigDecimal windowWeight(long d, int leadTimeDays) {
        if (d > leadTimeDays + 30L) {
            return BigDecimal.ZERO;
        }
        if (d >= leadTimeDays) {
            return BigDecimal.ONE;
        }
        if (d >= 0) {
            return HALF;
        }
        return BigDecimal.ZERO;
    }

    public static FestivalFactorResult compute(
            List<FestivalAffinityInput> affinities, int leadTimeDays, LocalDate evalDate) {
        BigDecimal best = BigDecimal.ZERO;
        String bestFestival = null;

        for (FestivalAffinityInput affinity : affinities) {
            long d = ChronoUnit.DAYS.between(evalDate, affinity.festivalDate());
            BigDecimal value = affinity.affinity().multiply(windowWeight(d, leadTimeDays));
            if (value.compareTo(best) > 0) {
                best = value;
                bestFestival = affinity.festivalCode();
            }
        }
        return new FestivalFactorResult(ScoreMath.round(best, 4), bestFestival);
    }
}
