package com.example.ssds.core.scoring;

import java.math.BigDecimal;
import java.util.List;

/**
 * `PRICE_FIT` 因子：建議售價與主力客群價格帶的重疊度（規格書 §5.2.4）。
 *
 * <pre>
 * fit(P, segment) = 1.0                              當 price_min ≤ P ≤ price_max
 *                 = max(0, 1 − dist / (band × 0.5))  否則
 *      dist = (P &lt; price_min) ? (price_min − P) : (P − price_max)
 *      band = price_max − price_min
 *
 * price_fit = Σ( share_s × fit(P, s) )   對該品類所有客群 s 加總，Σshare = 1
 * </pre>
 */
public final class PriceFitCalculator {

    private static final BigDecimal HALF = BigDecimal.valueOf(0.5);

    private PriceFitCalculator() {
    }

    public static BigDecimal priceFit(BigDecimal price, List<AudienceSegmentShare> segments) {
        BigDecimal total = BigDecimal.ZERO;
        for (AudienceSegmentShare segment : segments) {
            total = total.add(segment.share().multiply(fitOne(price, segment)));
        }
        return ScoreMath.round(total, 4);
    }

    private static BigDecimal fitOne(BigDecimal price, AudienceSegmentShare segment) {
        BigDecimal min = segment.priceMin();
        BigDecimal max = segment.priceMax();
        if (price.compareTo(min) >= 0 && price.compareTo(max) <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal dist = price.compareTo(min) < 0 ? min.subtract(price) : price.subtract(max);
        BigDecimal band = max.subtract(min);
        BigDecimal fit = BigDecimal.ONE.subtract(ScoreMath.safeDivide(dist, band.multiply(HALF)));
        return fit.max(BigDecimal.ZERO);
    }
}
