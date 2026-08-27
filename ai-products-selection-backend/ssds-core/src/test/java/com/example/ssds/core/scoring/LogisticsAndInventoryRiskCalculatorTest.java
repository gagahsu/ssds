package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ssds.core.domain.LogisticsCondition;
import com.example.ssds.core.domain.Season;
import java.time.Month;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 規格書 §5.5 黃金案例區塊 D：夏季高溫、易融化 → 扣 4；效期 180 天、MOQ 200 → 皆未觸發。 */
class LogisticsAndInventoryRiskCalculatorTest {

    @Test
    void goldenCase_summerMeltable_penalty4() {
        PenaltyContribution result = LogisticsRiskCalculator.calculate(
                Set.of(LogisticsCondition.MELTABLE), Month.JULY);
        assertThat(result.penaltyValue()).isEqualByComparingTo("4");
    }

    @Test
    void winterMeltable_noPenalty() {
        PenaltyContribution result = LogisticsRiskCalculator.calculate(
                Set.of(LogisticsCondition.MELTABLE), Month.NOVEMBER);
        assertThat(result.penaltyValue()).isEqualByComparingTo("0");
    }

    @Test
    void cappedAtMaxPenalty() {
        PenaltyContribution result = LogisticsRiskCalculator.calculate(
                Set.of(LogisticsCondition.MELTABLE, LogisticsCondition.CHILLED,
                        LogisticsCondition.FRAGILE, LogisticsCondition.OVERSIZED),
                Month.JULY);
        assertThat(result.penaltyValue()).isEqualByComparingTo("10");
    }

    @Test
    void goldenCase_normalShelfLifeAndMoq_noPenalty() {
        PenaltyContribution result = InventoryRiskCalculator.calculate(180, Season.ALL, 200);
        assertThat(result.penaltyValue()).isEqualByComparingTo("0");
    }

    @Test
    void shortShelfLife_triggersPenalty() {
        PenaltyContribution result = InventoryRiskCalculator.calculate(30, Season.ALL, 50);
        assertThat(result.penaltyValue()).isEqualByComparingTo("4");
    }
}
