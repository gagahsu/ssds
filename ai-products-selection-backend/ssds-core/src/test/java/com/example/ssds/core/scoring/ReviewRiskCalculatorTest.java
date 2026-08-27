package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 規格書 §5.2.2：評論風險扣分的四段判定 + 樣本不足退路。 */
class ReviewRiskCalculatorTest {

    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(0.15);

    @Test
    void goldenCase_belowThreshold_noPenalty() {
        // 負評率 10%，未達零食類門檻 15%（§5.5 黃金案例區塊 D）
        PenaltyContribution result = ReviewRiskCalculator.calculate(3, 30, THRESHOLD, BigDecimal.ZERO);
        assertThat(result.penaltyValue()).isEqualByComparingTo("0");
    }

    @Test
    void sampleTooSmall_noPenalty() {
        PenaltyContribution result = ReviewRiskCalculator.calculate(5, 19, THRESHOLD, BigDecimal.ONE);
        assertThat(result.penaltyValue()).isEqualByComparingTo("0");
        assertThat(result.note()).isEqualTo("評論樣本不足");
    }

    @Test
    void aboveThreshold_lowRiskTopicShare_penalty8() {
        PenaltyContribution result = ReviewRiskCalculator.calculate(8, 30, THRESHOLD, BigDecimal.valueOf(0.2));
        assertThat(result.penaltyValue()).isEqualByComparingTo("8");
    }

    @Test
    void aboveThreshold_midRiskTopicShare_penalty14() {
        PenaltyContribution result = ReviewRiskCalculator.calculate(8, 30, THRESHOLD, BigDecimal.valueOf(0.45));
        assertThat(result.penaltyValue()).isEqualByComparingTo("14");
    }

    @Test
    void aboveThreshold_highRiskTopicShare_penalty20() {
        PenaltyContribution result = ReviewRiskCalculator.calculate(8, 30, THRESHOLD, BigDecimal.valueOf(0.8));
        assertThat(result.penaltyValue()).isEqualByComparingTo("20");
    }
}
