package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ssds.core.domain.LogisticsCondition;
import com.example.ssds.core.domain.Season;
import java.math.BigDecimal;
import java.time.Month;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 規格書 §5.5 黃金案例區塊 D：夏季高溫、易融化 → 扣 4；效期 180 天、MOQ 200 → 皆未觸發。 */
class LogisticsAndInventoryRiskCalculatorTest {

    /** §5.2.2 初始經驗值（待客戶確認，附錄 A 第 18 項），供測試比照 risk_rule 的實際內容。 */
    private static final LogisticsRiskCalculator.Points LOGISTICS_POINTS = new LogisticsRiskCalculator.Points(
            BigDecimal.valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(3), BigDecimal.valueOf(3));

    private static final InventoryRiskCalculator.Thresholds INVENTORY_THRESHOLDS =
            new InventoryRiskCalculator.Thresholds(
                    60, BigDecimal.valueOf(4), BigDecimal.valueOf(3), 300, BigDecimal.valueOf(3));

    @Test
    void goldenCase_summerMeltable_penalty4() {
        PenaltyContribution result = LogisticsRiskCalculator.calculate(
                Set.of(LogisticsCondition.MELTABLE), Month.JULY, LOGISTICS_POINTS);
        assertThat(result.penaltyValue()).isEqualByComparingTo("4");
    }

    @Test
    void winterMeltable_noPenalty() {
        PenaltyContribution result = LogisticsRiskCalculator.calculate(
                Set.of(LogisticsCondition.MELTABLE), Month.NOVEMBER, LOGISTICS_POINTS);
        assertThat(result.penaltyValue()).isEqualByComparingTo("0");
    }

    @Test
    void cappedAtMaxPenalty() {
        PenaltyContribution result = LogisticsRiskCalculator.calculate(
                Set.of(LogisticsCondition.MELTABLE, LogisticsCondition.CHILLED,
                        LogisticsCondition.FRAGILE, LogisticsCondition.OVERSIZED),
                Month.JULY, LOGISTICS_POINTS);
        assertThat(result.penaltyValue()).isEqualByComparingTo("10");
    }

    @Test
    void goldenCase_normalShelfLifeAndMoq_noPenalty() {
        PenaltyContribution result = InventoryRiskCalculator.calculate(
                180, Season.ALL, 200, INVENTORY_THRESHOLDS);
        assertThat(result.penaltyValue()).isEqualByComparingTo("0");
    }

    @Test
    void shortShelfLife_triggersPenalty() {
        PenaltyContribution result = InventoryRiskCalculator.calculate(
                30, Season.ALL, 50, INVENTORY_THRESHOLDS);
        assertThat(result.penaltyValue()).isEqualByComparingTo("4");
    }
}
