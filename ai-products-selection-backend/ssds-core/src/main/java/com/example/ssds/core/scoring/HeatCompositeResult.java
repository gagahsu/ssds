package com.example.ssds.core.scoring;

import java.math.BigDecimal;

/**
 * @param compositeValue              null 表示所有來源皆不可用，無法合成（該日視為無資料）
 * @param degradedOrUnavailableCount  DEGRADED 或 UNAVAILABLE 的來源數，供信心度扣分（每個 −10）
 */
public record HeatCompositeResult(BigDecimal compositeValue, int degradedOrUnavailableCount) {
}
