package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 規格書 §5.3.3：雙窗口斜率 + 背離標記。 */
class TrendSlopeCalculatorTest {

    @Test
    void goldenCase_340PercentSurge() {
        // heat_t=440, heat_{t-7}=100 → slope_7d = (440-100)/100 = 3.40 = +340%
        TrendSlopeResult result = TrendSlopeCalculator.compute(
                BigDecimal.valueOf(440), BigDecimal.valueOf(100), BigDecimal.valueOf(200), false);

        assertThat(result.slope7d().doubleValue()).isCloseTo(3.40, within(0.001));
        assertThat(result.divergence()).isFalse();
    }

    @Test
    void divergence_shortTermDown_longTermUp() {
        TrendSlopeResult result = TrendSlopeCalculator.compute(
                BigDecimal.valueOf(90), BigDecimal.valueOf(100), BigDecimal.valueOf(50), false);

        assertThat(result.slope7d().signum()).isNegative();
        assertThat(result.slope30d().signum()).isPositive();
        assertThat(result.divergence()).isTrue();
    }

    @Test
    void epsilonFloor_avoidsDivisionByZero() {
        TrendSlopeResult result = TrendSlopeCalculator.compute(
                BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.ZERO, true);
        assertThat(result.slope7d().doubleValue()).isCloseTo(10.0, within(0.001));
        assertThat(result.shortHistory()).isTrue();
    }
}
