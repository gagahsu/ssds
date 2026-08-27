package com.example.ssds.core.scoring;

import java.math.BigDecimal;

/**
 * @param divergence     slope_7d < 0 且 slope_30d > 0（可能見頂，不直接影響分數，只供 UI 標示）
 * @param shortHistory   熱度歷史不滿 30 日，slope_30d 以現有最長區間計算（信心度 −5）
 */
public record TrendSlopeResult(
        BigDecimal slope7d,
        BigDecimal slope30d,
        BigDecimal trendRaw,
        boolean divergence,
        boolean shortHistory
) {
}
