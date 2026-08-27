package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 規格書 §5.9：信心度起算 100，下限 0，黃金案例 100-10-4=86。 */
class ConfidenceCalculatorTest {

    @Test
    void goldenCase_86() {
        int confidence = ConfidenceCalculator.calculate(List.of(
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_IMPUTED_FACTOR
        ));
        assertThat(confidence).isEqualTo(86);
    }

    @Test
    void noReasons_full100() {
        assertThat(ConfidenceCalculator.calculate(List.of())).isEqualTo(100);
    }

    @Test
    void clampedAtZero_neverNegative() {
        int confidence = ConfidenceCalculator.calculate(List.of(
                ConfidencePenaltyReason.LOW_CATEGORY_SAMPLE,
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.LOW_SCENE_CONFIDENCE
        ));
        assertThat(confidence).isZero();
    }
}
