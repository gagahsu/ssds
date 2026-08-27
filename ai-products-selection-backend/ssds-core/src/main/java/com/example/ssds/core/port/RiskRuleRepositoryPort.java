package com.example.ssds.core.port;

/** 扣分規則門檻查詢（規格書 §7.2.5 risk_rule），由 SYS_ADMIN 維護。由 ssds-infra 實作。 */
public interface RiskRuleRepositoryPort {
    /** @param categoryId 非 null 時優先取該品類的覆寫值，找不到時退回全域預設 */
    RiskRuleConfig findConfig(String ruleCode, Long categoryId);
}
