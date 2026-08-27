package com.example.ssds.core.port;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 扣分規則的門檻參數，對應 {@code risk_rule.threshold_json}（規格書 §7.2.5）。
 *
 * <p>{@code categoryId} 非 null 時為該品類的覆寫值，null 為全域預設；
 * 查詢端負責「有品類覆寫用品類，否則用全域」的解析（由實作 {@link RiskRuleRepositoryPort} 的一方決定）。
 */
public record RiskRuleConfig(
        String ruleCode, Long categoryId, Map<String, BigDecimal> thresholds, BigDecimal maxPenalty
) {
}
