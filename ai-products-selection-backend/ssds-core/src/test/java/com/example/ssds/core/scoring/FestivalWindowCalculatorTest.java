package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 規格書 §FR-17-1 黃金案例：中秋前 60 天、進口食品 L=45 → window=1.0，關聯度 0.60 → 因子值 0.60。
 * 另涵蓋 §11.2「時間窗邊界」要求的 d=L、d=L+30、d=0、d=-1 四個邊界點。
 */
class FestivalWindowCalculatorTest {

    private static final int LEAD_TIME = 45;

    @Test
    void goldenCase_60DaysBeforeMidAutumn_importedFood() {
        LocalDate evalDate = LocalDate.of(2026, 8, 1);
        LocalDate festivalDate = evalDate.plusDays(60);
        List<FestivalAffinityInput> affinities = List.of(
                new FestivalAffinityInput("MID_AUTUMN", festivalDate, BigDecimal.valueOf(0.60)));

        FestivalFactorResult result = FestivalWindowCalculator.compute(affinities, LEAD_TIME, evalDate);

        assertThat(result.rawValue()).isEqualByComparingTo("0.6000");
        assertThat(result.effectiveFestivalCode()).isEqualTo("MID_AUTUMN");
    }

    @Test
    void windowBoundaries() {
        assertThat(FestivalWindowCalculator.windowWeight(LEAD_TIME, LEAD_TIME)).isEqualByComparingTo("1.0");
        assertThat(FestivalWindowCalculator.windowWeight(LEAD_TIME + 30L, LEAD_TIME)).isEqualByComparingTo("1.0");
        assertThat(FestivalWindowCalculator.windowWeight(LEAD_TIME + 31L, LEAD_TIME)).isEqualByComparingTo("0.0");
        assertThat(FestivalWindowCalculator.windowWeight(0, LEAD_TIME)).isEqualByComparingTo("0.5");
        assertThat(FestivalWindowCalculator.windowWeight(-1, LEAD_TIME)).isEqualByComparingTo("0.0");
    }

    @Test
    void multipleFestivals_takesMaximum() {
        LocalDate evalDate = LocalDate.of(2026, 8, 1);
        List<FestivalAffinityInput> affinities = List.of(
                new FestivalAffinityInput("MID_AUTUMN", evalDate.plusDays(60), BigDecimal.valueOf(0.60)),
                new FestivalAffinityInput("LUNAR_NEW_YEAR", evalDate.plusDays(200), BigDecimal.valueOf(0.90))
        );

        FestivalFactorResult result = FestivalWindowCalculator.compute(affinities, LEAD_TIME, evalDate);

        // LUNAR_NEW_YEAR 的 d=200 > L+30=75，window=0 → 值為 0；MID_AUTUMN 仍勝出
        assertThat(result.effectiveFestivalCode()).isEqualTo("MID_AUTUMN");
    }
}
