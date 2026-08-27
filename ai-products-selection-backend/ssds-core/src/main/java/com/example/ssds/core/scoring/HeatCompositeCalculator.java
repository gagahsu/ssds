package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.SourceAvailability;
import java.math.BigDecimal;
import java.util.List;

/**
 * 熱度多來源合成（規格書 §5.3.2）。
 *
 * <pre>heat_composite = Σ( effective_weight_i × percentile_within_source_i ) / Σ( effective_weight_i )</pre>
 *
 * <p>任一來源不可用時其 effective_weight 歸零，分母同步縮減，其餘按比例重新正規化——
 * 這正是除以「Σ有效權重」而非固定 1 所自動達成的效果，不需額外步驟。
 */
public final class HeatCompositeCalculator {

    private HeatCompositeCalculator() {
    }

    public static HeatCompositeResult compose(List<HeatSourceContribution> sources) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightTotal = BigDecimal.ZERO;
        int degradedOrUnavailable = 0;

        for (HeatSourceContribution source : sources) {
            if (source.availability() != SourceAvailability.AVAILABLE) {
                degradedOrUnavailable++;
            }
            BigDecimal effectiveWeight = source.effectiveWeight();
            weightTotal = weightTotal.add(effectiveWeight);
            weightedSum = weightedSum.add(effectiveWeight.multiply(source.percentileWithinSource()));
        }

        BigDecimal composite = ScoreMath.safeDivide(weightedSum, weightTotal);
        return new HeatCompositeResult(
                composite == null ? null : ScoreMath.round(composite, 2), degradedOrUnavailable);
    }
}
