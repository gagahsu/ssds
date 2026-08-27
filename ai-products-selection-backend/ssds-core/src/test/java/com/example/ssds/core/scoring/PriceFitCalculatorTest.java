package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 規格書 §5.2.4 黃金案例：日式抹茶夾心餅乾，售價 149 元。 */
class PriceFitCalculatorTest {

    @Test
    void goldenCase_matcha_cookie_priceFit() {
        List<AudienceSegmentShare> segments = List.of(
                new AudienceSegmentShare("MAIN", BigDecimal.valueOf(120), BigDecimal.valueOf(180), BigDecimal.valueOf(0.6)),
                new AudienceSegmentShare("PRICE_SENSITIVE", BigDecimal.valueOf(60), BigDecimal.valueOf(120), BigDecimal.valueOf(0.3)),
                new AudienceSegmentShare("PREMIUM", BigDecimal.valueOf(180), BigDecimal.valueOf(400), BigDecimal.valueOf(0.1))
        );

        BigDecimal priceFit = PriceFitCalculator.priceFit(BigDecimal.valueOf(149), segments);

        assertThat(priceFit.doubleValue()).isCloseTo(0.682, org.assertj.core.api.Assertions.within(0.001));
    }
}
