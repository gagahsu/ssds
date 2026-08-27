package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 規格書 §FR-17-2 黃金案例：日式抹茶夾心餅乾，適溫 18–26°C，8 月台北歷史月均溫 29.4°C。 */
class ClimateFitCalculatorTest {

    @Test
    void goldenCase_taipeiAugust_outsideIdealRange() {
        BigDecimal fit = ClimateFitCalculator.fit(
                BigDecimal.valueOf(29.4), BigDecimal.valueOf(18), BigDecimal.valueOf(26),
                ClimateFitCalculator.DEFAULT_TOLERANCE);

        assertThat(fit.doubleValue()).isCloseTo(0.717, within(0.001));
    }

    @Test
    void withinIdealRange_returnsOne() {
        BigDecimal fit = ClimateFitCalculator.fit(
                BigDecimal.valueOf(22), BigDecimal.valueOf(18), BigDecimal.valueOf(26),
                ClimateFitCalculator.DEFAULT_TOLERANCE);

        assertThat(fit).isEqualByComparingTo(BigDecimal.ONE);
    }
}
