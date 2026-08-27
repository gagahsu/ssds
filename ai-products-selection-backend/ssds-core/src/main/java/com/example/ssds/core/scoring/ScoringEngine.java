package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 選品推薦分數的加權公式（規格書 §5.5）。
 *
 * <pre>
 * 加分小計 = Σ( w_i × normalized_i )   Σw_i = 1，normalized_i ∈ [0, 100]，結果直接落在 [0, 100]
 * 扣分小計 = Σ( penalty_j )            正值，上限 40
 * 選品分數 = max(0, 加分小計 − 扣分小計)
 * </pre>
 *
 * <p>v3.0 明確取消二次正規化：加權和本身已是百分位的加權平均，值域即為 0–100（§5.5 裁決）。
 *
 * <p>六項加分因子缺 4 項以上（過半）時不產生分數，見 {@link ScoringResult#sufficientData()}（§5.7）。
 */
public final class ScoringEngine {

    /** 六項加分因子缺此數以上（含）時不產生分數（規格書 §5.7）。 */
    private static final int MIN_MISSING_FOR_NO_SCORE = 4;

    private ScoringEngine() {
    }

    public static ScoringResult score(
            Map<FactorCode, BigDecimal> baseWeights,
            List<BonusFactorInput> bonusInputs,
            List<PenaltyContribution> penaltyContributions,
            GradeThresholdSet gradeThresholds,
            int confidence) {

        Set<FactorCode> available = EnumSet.noneOf(FactorCode.class);
        for (BonusFactorInput input : bonusInputs) {
            if (input.dataAvailable()) {
                available.add(input.factorCode());
            }
        }

        int missing = bonusInputs.size() - available.size();
        BigDecimal penaltySubtotal = sumPenalties(penaltyContributions);

        if (missing >= MIN_MISSING_FOR_NO_SCORE) {
            List<BonusFactorContribution> emptyContributions = new ArrayList<>();
            for (BonusFactorInput input : bonusInputs) {
                emptyContributions.add(new BonusFactorContribution(
                        input.factorCode(), input.rawValue(), input.normalizedValue(),
                        BigDecimal.ZERO, BigDecimal.ZERO, input.dataAvailable(), input.imputed(),
                        input.note()));
            }
            return new ScoringResult(false, null, penaltySubtotal, null, null,
                    emptyContributions, penaltyContributions);
        }

        Map<FactorCode, BigDecimal> reallocated = WeightAllocator.reallocate(baseWeights, available);

        List<BonusFactorContribution> contributions = new ArrayList<>();
        BigDecimal bonusSubtotal = BigDecimal.ZERO;
        for (BonusFactorInput input : bonusInputs) {
            if (!input.dataAvailable()) {
                contributions.add(new BonusFactorContribution(
                        input.factorCode(), null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                        false, false, input.note()));
                continue;
            }
            BigDecimal weight = reallocated.get(input.factorCode());
            BigDecimal contribution = weight.multiply(input.normalizedValue());
            bonusSubtotal = bonusSubtotal.add(contribution);
            contributions.add(new BonusFactorContribution(
                    input.factorCode(), input.rawValue(), input.normalizedValue(),
                    ScoreMath.round(weight, 3), ScoreMath.round(contribution, 2),
                    true, input.imputed(), input.note()));
        }
        bonusSubtotal = ScoreMath.round(bonusSubtotal, 2);

        BigDecimal finalScore = bonusSubtotal.subtract(penaltySubtotal).max(BigDecimal.ZERO);
        finalScore = ScoreMath.round(finalScore, 2);

        var grade = GradeClassifier.classify(finalScore, gradeThresholds, penaltySubtotal);

        return new ScoringResult(true, bonusSubtotal, penaltySubtotal, finalScore, grade,
                contributions, penaltyContributions);
    }

    private static BigDecimal sumPenalties(List<PenaltyContribution> penalties) {
        BigDecimal sum = BigDecimal.ZERO;
        for (PenaltyContribution p : penalties) {
            sum = sum.add(p.penaltyValue());
        }
        return ScoreMath.round(sum, 2);
    }
}
