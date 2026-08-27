package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.LogisticsCondition;
import java.math.BigDecimal;
import java.time.Month;
import java.util.Set;

/**
 * `LOGISTICS_RISK` 扣分（規格書 §5.2.2：冷鏈需求、易融化、易碎、材積異常，上限 10）。
 *
 * <p>各條件的扣分點數由呼叫端經 {@code RiskRuleRepositoryPort} 從
 * {@code risk_rule.threshold_json} 取得（見 {@link Points}），不寫死在本類別——
 * 上限（10）本身是 §5.2 定義的因子結構常數（{@link FactorCode#maxPenalty()}），
 * 與 SYS_ADMIN 可調的點數表分屬不同性質，故不經 {@code risk_rule} 覆寫。
 */
public final class LogisticsRiskCalculator {

    private static final Set<Month> SUMMER_MONTHS =
            Set.of(Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER);

    private LogisticsRiskCalculator() {
    }

    /**
     * 各條件的扣分點數，對應 {@code risk_rule.threshold_json}（`rule_code = 'LOGISTICS_RISK'`）
     * 的 key：{@code meltable_summer_points}／{@code cold_chain_points}／
     * {@code fragile_points}／{@code oversized_points}（§5.2.2 初始經驗值，待客戶確認，
     * 附錄 A 第 18 項）。
     */
    public record Points(
            BigDecimal meltableSummer, BigDecimal coldChain, BigDecimal fragile, BigDecimal oversized) {
    }

    public static PenaltyContribution calculate(
            Set<LogisticsCondition> conditions, Month evaluationMonth, Points points) {
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder note = new StringBuilder();

        if (conditions.contains(LogisticsCondition.MELTABLE) && SUMMER_MONTHS.contains(evaluationMonth)) {
            total = total.add(points.meltableSummer());
            note.append("夏季高溫、易融化");
        }
        if (conditions.contains(LogisticsCondition.CHILLED) || conditions.contains(LogisticsCondition.FROZEN)) {
            total = total.add(points.coldChain());
            appendNote(note, "冷鏈需求");
        }
        if (conditions.contains(LogisticsCondition.FRAGILE)) {
            total = total.add(points.fragile());
            appendNote(note, "易碎");
        }
        if (conditions.contains(LogisticsCondition.OVERSIZED)) {
            total = total.add(points.oversized());
            appendNote(note, "材積異常");
        }

        BigDecimal maxPenalty = BigDecimal.valueOf(FactorCode.LOGISTICS_RISK.maxPenalty());
        BigDecimal penalty = total.min(maxPenalty);
        return new PenaltyContribution(FactorCode.LOGISTICS_RISK, penalty,
                note.isEmpty() ? "無命中之物流條件" : note.toString());
    }

    private static void appendNote(StringBuilder note, String text) {
        if (!note.isEmpty()) {
            note.append("、");
        }
        note.append(text);
    }
}
