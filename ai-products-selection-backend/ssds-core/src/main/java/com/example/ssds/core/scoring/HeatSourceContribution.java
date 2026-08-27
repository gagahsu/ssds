package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.HeatSourceGranularity;
import com.example.ssds.core.domain.SourceAvailability;
import java.math.BigDecimal;

/** 單一熱度來源當日的合成輸入（規格書 §5.3.2）。 */
public record HeatSourceContribution(
        String sourceCode,
        BigDecimal configuredWeight,
        BigDecimal percentileWithinSource,
        SourceAvailability availability,
        HeatSourceGranularity granularity
) {
    /** effective_weight = configured_weight × (不可用?0:1) × (品類級?0.5:1)（§5.3.2）。 */
    public BigDecimal effectiveWeight() {
        if (availability == SourceAvailability.UNAVAILABLE) {
            return BigDecimal.ZERO;
        }
        BigDecimal granularityDiscount = granularity == HeatSourceGranularity.CATEGORY
                ? BigDecimal.valueOf(0.5) : BigDecimal.ONE;
        return configuredWeight.multiply(granularityDiscount);
    }
}
