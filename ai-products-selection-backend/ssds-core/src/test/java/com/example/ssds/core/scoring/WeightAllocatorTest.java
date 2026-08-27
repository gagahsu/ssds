package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.ssds.core.domain.FactorCode;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 規格書 §5.7：資料不足時的權重分攤，w'_i = w_i / Σ(w_k for k in available)。 */
class WeightAllocatorTest {

    @Test
    void missingOneFactor_reallocatesProportionally() {
        Map<FactorCode, BigDecimal> base = new EnumMap<>(FactorCode.class);
        base.put(FactorCode.TREND, BigDecimal.valueOf(0.5));
        base.put(FactorCode.MARGIN, BigDecimal.valueOf(0.1));
        base.put(FactorCode.CVR, BigDecimal.valueOf(0.4));

        // CVR 缺資料，剩 TREND/MARGIN，總和 0.6，分攤後 TREND=0.5/0.6, MARGIN=0.1/0.6
        Map<FactorCode, BigDecimal> result =
                WeightAllocator.reallocate(base, EnumSet.of(FactorCode.TREND, FactorCode.MARGIN));

        assertThat(result).doesNotContainKey(FactorCode.CVR);
        assertThat(result.get(FactorCode.TREND).doubleValue()).isCloseTo(0.8333, within(0.001));
        assertThat(result.get(FactorCode.MARGIN).doubleValue()).isCloseTo(0.1667, within(0.001));

        BigDecimal sum = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum.doubleValue()).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void allAvailable_weightsUnchanged() {
        Map<FactorCode, BigDecimal> base = new EnumMap<>(FactorCode.class);
        base.put(FactorCode.TREND, BigDecimal.valueOf(0.5));
        base.put(FactorCode.MARGIN, BigDecimal.valueOf(0.5));

        Map<FactorCode, BigDecimal> result =
                WeightAllocator.reallocate(base, EnumSet.of(FactorCode.TREND, FactorCode.MARGIN));

        assertThat(result.get(FactorCode.TREND).doubleValue()).isCloseTo(0.5, within(0.0001));
    }
}
