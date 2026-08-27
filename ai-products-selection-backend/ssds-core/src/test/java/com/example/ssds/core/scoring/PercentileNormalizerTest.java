package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 規格書 §5.3.1：平均排名法 + 樣本不足三段退路。 */
class PercentileNormalizerTest {

    @Test
    void tiedRanks_useAverageRankMethod() {
        // population [1,2,2,3], value=2: less=1, equal=2, n=4 → (1+1)/4=0.5 → 50
        BigDecimal percentile = PercentileNormalizer.percentileRank(
                BigDecimal.valueOf(2),
                List.of(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
        assertThat(percentile).isEqualByComparingTo("50.00");
    }

    @Test
    void ownCategorySampleAtLeast10_usesOwnCategoryTier() {
        List<BigDecimal> own = sample(10);
        NormalizationResult result = PercentileNormalizer.normalize(
                BigDecimal.valueOf(5), own, List.of(), List.of());
        assertThat(result.tier()).isEqualTo(NormalizationTier.OWN_CATEGORY);
        assertThat(result.confidencePenaltyReason()).isNull();
    }

    @Test
    void ownCategorySample3to9_fallsBackToSiblingMerged() {
        List<BigDecimal> own = sample(5);
        List<BigDecimal> sibling = sample(20);
        NormalizationResult result = PercentileNormalizer.normalize(
                BigDecimal.valueOf(5), own, sibling, List.of());
        assertThat(result.tier()).isEqualTo(NormalizationTier.SIBLING_MERGED);
        assertThat(result.confidencePenaltyReason()).isEqualTo(ConfidencePenaltyReason.LOW_CATEGORY_SAMPLE);
    }

    @Test
    void ownCategorySampleBelow3_fallsBackToAllCategory() {
        List<BigDecimal> own = sample(2);
        List<BigDecimal> all = sample(100);
        NormalizationResult result = PercentileNormalizer.normalize(
                BigDecimal.valueOf(5), own, List.of(), all);
        assertThat(result.tier()).isEqualTo(NormalizationTier.ALL_CATEGORY);
        assertThat(result.confidencePenaltyReason()).isEqualTo(ConfidencePenaltyReason.LOW_CATEGORY_SAMPLE);
    }

    private static List<BigDecimal> sample(int n) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(BigDecimal::valueOf)
                .toList();
    }
}
