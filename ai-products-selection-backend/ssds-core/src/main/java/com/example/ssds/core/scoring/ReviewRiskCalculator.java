package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import java.math.BigDecimal;

/**
 * `REVIEW_RISK` 扣分（規格書 §5.2.2）。
 *
 * <pre>
 * negative_rate = 負面評論則數 / 總評論則數
 * risk_topic_share = 負評中屬於「品質／食安／物流破損」三類的比例
 *
 * negative_rate ≤ category_threshold                          → 扣 0
 * negative_rate &gt; category_threshold 且 risk_topic_share &lt; 0.3     → 扣 8
 * negative_rate &gt; category_threshold 且 0.3 ≤ risk_topic_share &lt; 0.6 → 扣 14
 * negative_rate &gt; category_threshold 且 risk_topic_share ≥ 0.6     → 扣 20
 * </pre>
 *
 * <p>評論則數 &lt; 20 時不計算（樣本不足），扣 0 並標示。
 */
public final class ReviewRiskCalculator {

    private static final int MIN_SAMPLE_SIZE = 20;
    private static final BigDecimal LOW_SHARE = BigDecimal.valueOf(0.3);
    private static final BigDecimal HIGH_SHARE = BigDecimal.valueOf(0.6);

    private ReviewRiskCalculator() {
    }

    public static PenaltyContribution calculate(
            int negativeCount, int totalCount, BigDecimal categoryThreshold, BigDecimal riskTopicShare) {
        if (totalCount < MIN_SAMPLE_SIZE) {
            return new PenaltyContribution(FactorCode.REVIEW_RISK, BigDecimal.ZERO, "評論樣本不足");
        }

        BigDecimal negativeRate = ScoreMath.safeDivide(
                BigDecimal.valueOf(negativeCount), BigDecimal.valueOf(totalCount));

        if (negativeRate.compareTo(categoryThreshold) <= 0) {
            return new PenaltyContribution(FactorCode.REVIEW_RISK, BigDecimal.ZERO,
                    "負評率 %.0f%%，未達品類門檻 %.0f%%".formatted(
                            negativeRate.movePointRight(2), categoryThreshold.movePointRight(2)));
        }

        BigDecimal penalty;
        if (riskTopicShare.compareTo(LOW_SHARE) < 0) {
            penalty = BigDecimal.valueOf(8);
        } else if (riskTopicShare.compareTo(HIGH_SHARE) < 0) {
            penalty = BigDecimal.valueOf(14);
        } else {
            penalty = BigDecimal.valueOf(20);
        }
        return new PenaltyContribution(FactorCode.REVIEW_RISK, penalty,
                "負評率超標，風險主題佔比 %.0f%%".formatted(riskTopicShare.movePointRight(2)));
    }
}
