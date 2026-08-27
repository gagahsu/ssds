package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 資料不足時的權重分攤（規格書 §5.7）。
 *
 * <pre>
 * available = 有資料的因子集合
 * w'_i = w_i / Σ(w_k for k in available)      使 Σw'_i = 1
 * </pre>
 */
public final class WeightAllocator {

    private WeightAllocator() {
    }

    /**
     * @param baseWeights 該情境的原始權重組，六項加總為 1
     * @param available   {@code data_available = true} 的因子集合
     * @return 只含 available 因子的分攤後權重；available 為空時回傳空 map
     */
    public static Map<FactorCode, BigDecimal> reallocate(
            Map<FactorCode, BigDecimal> baseWeights, Set<FactorCode> available) {
        BigDecimal sumAvailable = BigDecimal.ZERO;
        for (FactorCode code : available) {
            sumAvailable = sumAvailable.add(baseWeights.getOrDefault(code, BigDecimal.ZERO));
        }

        Map<FactorCode, BigDecimal> result = new EnumMap<>(FactorCode.class);
        if (available.isEmpty() || sumAvailable.signum() == 0) {
            return result;
        }
        for (FactorCode code : available) {
            BigDecimal w = baseWeights.getOrDefault(code, BigDecimal.ZERO);
            result.put(code, ScoreMath.safeDivide(w, sumAvailable));
        }
        return result;
    }
}
