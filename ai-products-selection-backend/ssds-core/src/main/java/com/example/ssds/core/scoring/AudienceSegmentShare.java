package com.example.ssds.core.scoring;

import java.math.BigDecimal;

/** 品類客群組成的一列（規格書 §7.2.2 category_audience_mix × audience_segment）。 */
public record AudienceSegmentShare(
        String audienceCode, BigDecimal priceMin, BigDecimal priceMax, BigDecimal share
) {
}
