package com.example.ssds.core.scoring;

import java.math.BigDecimal;
import java.util.List;

/**
 * 同品類百分位正規化（規格書 §5.3.1）。
 *
 * <pre>normalized(x) = percentile_rank(x, same_category_values) × 100</pre>
 *
 * <p>並列一律採平均排名法：{@code (count_less + 0.5 × count_equal) / n}。
 *
 * <p>樣本不足退路：≥10 直接用同品類；3–9 與兄弟品類合併；&lt;3（含合併後仍不足）用全品類，
 * 後兩者信心度扣 20（§5.9）。
 */
public final class PercentileNormalizer {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal HALF = BigDecimal.valueOf(0.5);
    private static final int OWN_CATEGORY_MIN = 10;
    private static final int SIBLING_MERGED_MIN = 3;

    private PercentileNormalizer() {
    }

    public static NormalizationResult normalize(
            BigDecimal value,
            List<BigDecimal> ownCategoryPopulation,
            List<BigDecimal> siblingMergedPopulation,
            List<BigDecimal> allCategoryPopulation) {

        NormalizationTier tier;
        List<BigDecimal> population;
        ConfidencePenaltyReason penaltyReason = null;
        String note = null;

        if (ownCategoryPopulation.size() >= OWN_CATEGORY_MIN) {
            tier = NormalizationTier.OWN_CATEGORY;
            population = ownCategoryPopulation;
        } else if (ownCategoryPopulation.size() >= SIBLING_MERGED_MIN) {
            tier = NormalizationTier.SIBLING_MERGED;
            population = siblingMergedPopulation;
            penaltyReason = ConfidencePenaltyReason.LOW_CATEGORY_SAMPLE;
            note = "與同一父品類下的兄弟品類合併計算";
        } else {
            tier = NormalizationTier.ALL_CATEGORY;
            population = allCategoryPopulation;
            penaltyReason = ConfidencePenaltyReason.LOW_CATEGORY_SAMPLE;
            note = "本因子以全品類基準計算，跨品類量級差異可能影響排序";
        }

        BigDecimal percentile = percentileRank(value, population);
        return new NormalizationResult(percentile, tier, penaltyReason, note);
    }

    /** 平均排名法：(count_less + 0.5 × count_equal) / n × 100。population 為空時回傳 null。 */
    public static BigDecimal percentileRank(BigDecimal value, List<BigDecimal> population) {
        if (population.isEmpty()) {
            return null;
        }
        int less = 0;
        int equal = 0;
        for (BigDecimal candidate : population) {
            int cmp = candidate.compareTo(value);
            if (cmp < 0) {
                less++;
            } else if (cmp == 0) {
                equal++;
            }
        }
        BigDecimal rank = BigDecimal.valueOf(less).add(HALF.multiply(BigDecimal.valueOf(equal)));
        BigDecimal ratio = ScoreMath.safeDivide(rank, BigDecimal.valueOf(population.size()));
        return ScoreMath.round(ratio.multiply(HUNDRED), 2);
    }
}
