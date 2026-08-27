package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.Grade;
import com.example.ssds.core.domain.SceneType;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 規格書 §5.5「日式抹茶夾心餅乾」黃金案例，§11.1 迴歸測試的固定案例。
 * 任何評分邏輯變更都必須通過本測試。
 */
class ScoringEngineGoldenCaseTest {

    // §5.6：話題爆款榜 A ≥ 85、B 70–84
    private static final GradeThresholdSet VIRAL_THRESHOLDS =
            new GradeThresholdSet(SceneType.VIRAL, BigDecimal.valueOf(85), BigDecimal.valueOf(70));

    private static Map<FactorCode, BigDecimal> goldenWeights() {
        Map<FactorCode, BigDecimal> weights = new EnumMap<>(FactorCode.class);
        weights.put(FactorCode.TREND, BigDecimal.valueOf(0.50));
        weights.put(FactorCode.MARGIN, BigDecimal.valueOf(0.10));
        weights.put(FactorCode.CVR, BigDecimal.valueOf(0.08));
        weights.put(FactorCode.PRICE_FIT, BigDecimal.valueOf(0.07));
        weights.put(FactorCode.FESTIVAL, BigDecimal.valueOf(0.15));
        weights.put(FactorCode.CLIMATE, BigDecimal.valueOf(0.10));
        return weights;
    }

    private static List<BonusFactorInput> bonusInputs(BigDecimal climateNormalized) {
        return List.of(
                new BonusFactorInput(FactorCode.TREND, BigDecimal.valueOf(3.40), BigDecimal.valueOf(96), true, false, null),
                new BonusFactorInput(FactorCode.MARGIN, BigDecimal.valueOf(0.38), BigDecimal.valueOf(88), true, false, null),
                new BonusFactorInput(FactorCode.CVR, BigDecimal.valueOf(0.042), BigDecimal.valueOf(90), true, false, null),
                new BonusFactorInput(FactorCode.PRICE_FIT, BigDecimal.valueOf(0.68), BigDecimal.valueOf(82), true, false, null),
                new BonusFactorInput(FactorCode.FESTIVAL, BigDecimal.valueOf(0.60), BigDecimal.valueOf(85), true, false, null),
                new BonusFactorInput(FactorCode.CLIMATE, BigDecimal.valueOf(0.717), climateNormalized, true, false, null)
        );
    }

    @Test
    void julyEvaluation_bonusSubtotal_penaltySubtotal_finalScore_grade_confidence() {
        List<PenaltyContribution> penalties = List.of(
                new PenaltyContribution(FactorCode.REVIEW_RISK, BigDecimal.ZERO, "負評率 10%，未達零食類門檻 15%"),
                new PenaltyContribution(FactorCode.LOGISTICS_RISK, BigDecimal.valueOf(4.00), "夏季高溫、易融化"),
                new PenaltyContribution(FactorCode.INVENTORY_RISK, BigDecimal.ZERO, "效期 180 天、MOQ 200，皆未觸發")
        );

        // §5.9 黃金案例驗算：熱度來源 Instagram 當日降級（-10）、CVR 為推估值（-4）= 100-14 = 86
        int confidence = ConfidenceCalculator.calculate(List.of(
                ConfidencePenaltyReason.PER_DEGRADED_HEAT_SOURCE,
                ConfidencePenaltyReason.PER_IMPUTED_FACTOR
        ));
        assertThat(confidence).isEqualTo(86);

        ScoringResult result = ScoringEngine.score(
                goldenWeights(), bonusInputs(BigDecimal.valueOf(44)), penalties, VIRAL_THRESHOLDS, confidence);

        assertThat(result.sufficientData()).isTrue();
        assertThat(result.bonusSubtotal()).isEqualByComparingTo("86.89");
        assertThat(result.penaltySubtotal()).isEqualByComparingTo("4.00");
        assertThat(result.finalScore()).isEqualByComparingTo("82.89");
        assertThat(result.grade()).isEqualTo(Grade.B);

        BigDecimal contributionSum = result.factorContributions().stream()
                .map(BonusFactorContribution::contribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(contributionSum.doubleValue()).isCloseTo(86.89, within(0.01));

        BigDecimal weightSum = result.factorContributions().stream()
                .map(BonusFactorContribution::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(weightSum.doubleValue()).isCloseTo(1.000, within(0.001));
    }

    @Test
    void novemberVariant_climateImproves_noLogisticsPenalty_gradeA() {
        // §5.5 對照：CLIMATE 百分位 44 → 82，無夏季物流扣分
        List<PenaltyContribution> penalties = List.of(
                new PenaltyContribution(FactorCode.REVIEW_RISK, BigDecimal.ZERO, "未達門檻"),
                new PenaltyContribution(FactorCode.LOGISTICS_RISK, BigDecimal.ZERO, "非夏季，無易融化風險"),
                new PenaltyContribution(FactorCode.INVENTORY_RISK, BigDecimal.ZERO, "皆未觸發")
        );

        ScoringResult result = ScoringEngine.score(
                goldenWeights(), bonusInputs(BigDecimal.valueOf(82)), penalties, VIRAL_THRESHOLDS, 86);

        assertThat(result.bonusSubtotal()).isEqualByComparingTo("90.69");
        assertThat(result.penaltySubtotal()).isEqualByComparingTo("0.00");
        assertThat(result.finalScore()).isEqualByComparingTo("90.69");
        assertThat(result.grade()).isEqualTo(Grade.A);
    }
}
