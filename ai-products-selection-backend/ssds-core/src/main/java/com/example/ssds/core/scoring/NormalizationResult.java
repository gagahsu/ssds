package com.example.ssds.core.scoring;

import java.math.BigDecimal;

/**
 * @param confidencePenaltyReason 非 OWN_CATEGORY 時為 {@link ConfidencePenaltyReason#LOW_CATEGORY_SAMPLE}，否則為 null
 */
public record NormalizationResult(
        BigDecimal percentile,
        NormalizationTier tier,
        ConfidencePenaltyReason confidencePenaltyReason,
        String note
) {
}
