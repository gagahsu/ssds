package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.SceneType;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 規格書 §5.7：六項加分因子缺 4 項以上（過半）時不產生分數。 */
class ScoringEngineInsufficientDataTest {

    private static final GradeThresholdSet THRESHOLDS =
            new GradeThresholdSet(SceneType.REPLENISHMENT, BigDecimal.valueOf(80), BigDecimal.valueOf(65));

    @Test
    void fourOfSixMissing_noScoreProduced() {
        List<BonusFactorInput> inputs = List.of(
                new BonusFactorInput(FactorCode.TREND, BigDecimal.TEN, BigDecimal.valueOf(50), true, false, null),
                new BonusFactorInput(FactorCode.MARGIN, BigDecimal.TEN, BigDecimal.valueOf(50), true, false, null),
                BonusFactorInput.unavailable(FactorCode.CVR, "無資料"),
                BonusFactorInput.unavailable(FactorCode.PRICE_FIT, "無資料"),
                BonusFactorInput.unavailable(FactorCode.FESTIVAL, "無資料"),
                BonusFactorInput.unavailable(FactorCode.CLIMATE, "無資料")
        );
        Map<FactorCode, BigDecimal> weights = new EnumMap<>(FactorCode.class);
        weights.put(FactorCode.TREND, BigDecimal.valueOf(0.5));
        weights.put(FactorCode.MARGIN, BigDecimal.valueOf(0.5));

        ScoringResult result = ScoringEngine.score(weights, inputs, List.of(), THRESHOLDS, 60);

        assertThat(result.sufficientData()).isFalse();
        assertThat(result.bonusSubtotal()).isNull();
        assertThat(result.finalScore()).isNull();
        assertThat(result.grade()).isNull();
    }

    @Test
    void threeOfSixMissing_stillScores() {
        List<BonusFactorInput> inputs = List.of(
                new BonusFactorInput(FactorCode.TREND, BigDecimal.TEN, BigDecimal.valueOf(50), true, false, null),
                new BonusFactorInput(FactorCode.MARGIN, BigDecimal.TEN, BigDecimal.valueOf(50), true, false, null),
                new BonusFactorInput(FactorCode.CVR, BigDecimal.TEN, BigDecimal.valueOf(50), true, false, null),
                BonusFactorInput.unavailable(FactorCode.PRICE_FIT, "無資料"),
                BonusFactorInput.unavailable(FactorCode.FESTIVAL, "無資料"),
                BonusFactorInput.unavailable(FactorCode.CLIMATE, "無資料")
        );
        Map<FactorCode, BigDecimal> weights = new EnumMap<>(FactorCode.class);
        weights.put(FactorCode.TREND, BigDecimal.valueOf(0.4));
        weights.put(FactorCode.MARGIN, BigDecimal.valueOf(0.3));
        weights.put(FactorCode.CVR, BigDecimal.valueOf(0.3));

        ScoringResult result = ScoringEngine.score(weights, inputs, List.of(), THRESHOLDS, 60);

        assertThat(result.sufficientData()).isTrue();
        assertThat(result.bonusSubtotal()).isEqualByComparingTo("50.00");
    }
}
