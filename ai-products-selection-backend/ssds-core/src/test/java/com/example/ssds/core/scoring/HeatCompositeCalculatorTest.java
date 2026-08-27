package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.ssds.core.domain.HeatSourceGranularity;
import com.example.ssds.core.domain.SourceAvailability;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 規格書 §5.3.2：熱度多來源合成，不可用來源歸零、其餘按比例重新正規化。 */
class HeatCompositeCalculatorTest {

    @Test
    void allAvailable_weightedAverage() {
        List<HeatSourceContribution> sources = List.of(
                new HeatSourceContribution("THREADS", BigDecimal.valueOf(0.6), BigDecimal.valueOf(80),
                        SourceAvailability.AVAILABLE, HeatSourceGranularity.KEYWORD),
                new HeatSourceContribution("GOOGLE_TRENDS", BigDecimal.valueOf(0.4), BigDecimal.valueOf(60),
                        SourceAvailability.AVAILABLE, HeatSourceGranularity.KEYWORD)
        );

        HeatCompositeResult result = HeatCompositeCalculator.compose(sources);

        // (0.6*80 + 0.4*60) / 1.0 = 72
        assertThat(result.compositeValue().doubleValue()).isCloseTo(72.0, within(0.01));
        assertThat(result.degradedOrUnavailableCount()).isZero();
    }

    @Test
    void unavailableSource_zeroedAndRestRenormalized() {
        List<HeatSourceContribution> sources = List.of(
                new HeatSourceContribution("THREADS", BigDecimal.valueOf(0.6), BigDecimal.valueOf(80),
                        SourceAvailability.UNAVAILABLE, HeatSourceGranularity.KEYWORD),
                new HeatSourceContribution("GOOGLE_TRENDS", BigDecimal.valueOf(0.4), BigDecimal.valueOf(60),
                        SourceAvailability.AVAILABLE, HeatSourceGranularity.KEYWORD)
        );

        HeatCompositeResult result = HeatCompositeCalculator.compose(sources);

        // 分母縮減為 0.4 → composite = 0.4*60/0.4 = 60（等同僅剩來源自己重新正規化）
        assertThat(result.compositeValue().doubleValue()).isCloseTo(60.0, within(0.01));
        assertThat(result.degradedOrUnavailableCount()).isEqualTo(1);
    }

    @Test
    void categoryGranularity_appliesHalfDiscount() {
        List<HeatSourceContribution> sources = List.of(
                new HeatSourceContribution("INSTAGRAM", BigDecimal.valueOf(0.2), BigDecimal.valueOf(100),
                        SourceAvailability.AVAILABLE, HeatSourceGranularity.CATEGORY),
                new HeatSourceContribution("THREADS", BigDecimal.valueOf(0.8), BigDecimal.valueOf(50),
                        SourceAvailability.AVAILABLE, HeatSourceGranularity.KEYWORD)
        );
        // effective weights: IG 0.2*0.5=0.1, THREADS 0.8*1=0.8, total 0.9
        // composite = (0.1*100 + 0.8*50) / 0.9 = (10+40)/0.9 = 55.56
        HeatCompositeResult result = HeatCompositeCalculator.compose(sources);
        assertThat(result.compositeValue().doubleValue()).isCloseTo(55.56, within(0.01));
    }

    @Test
    void allUnavailable_noCompositeValue() {
        List<HeatSourceContribution> sources = List.of(
                new HeatSourceContribution("THREADS", BigDecimal.valueOf(1.0), BigDecimal.valueOf(80),
                        SourceAvailability.UNAVAILABLE, HeatSourceGranularity.KEYWORD)
        );
        HeatCompositeResult result = HeatCompositeCalculator.compose(sources);
        assertThat(result.compositeValue()).isNull();
    }
}
