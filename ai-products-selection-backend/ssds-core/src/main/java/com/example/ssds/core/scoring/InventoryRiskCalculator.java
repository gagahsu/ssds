package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.Season;
import java.math.BigDecimal;

/**
 * `INVENTORY_RISK` 扣分（規格書 §5.2.2：效期短、季節性強、最小訂購量偏高，上限 10）。
 *
 * <p>觸發門檻與扣分點數由呼叫端經 {@code RiskRuleRepositoryPort} 從
 * {@code risk_rule.threshold_json} 取得（見 {@link Thresholds}），不寫死在本類別——
 * 理由同 {@link LogisticsRiskCalculator}。
 */
public final class InventoryRiskCalculator {

    private InventoryRiskCalculator() {
    }

    /**
     * 對應 {@code risk_rule.threshold_json}（`rule_code = 'INVENTORY_RISK'`）的 key：
     * {@code short_shelf_life_days}／{@code short_shelf_life_points}／
     * {@code seasonal_points}／{@code high_moq}／{@code high_moq_points}
     * （§5.2.2 初始經驗值，待客戶確認，附錄 A 第 18 項）。
     */
    public record Thresholds(
            int shortShelfLifeDays, BigDecimal shortShelfLifePoints,
            BigDecimal seasonalPoints,
            int highMoq, BigDecimal highMoqPoints) {
    }

    public static PenaltyContribution calculate(
            Integer shelfLifeDays, Season season, Integer moq, Thresholds thresholds) {
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder note = new StringBuilder();

        if (shelfLifeDays != null && shelfLifeDays < thresholds.shortShelfLifeDays()) {
            total = total.add(thresholds.shortShelfLifePoints());
            note.append("效期短");
        }
        if (season != Season.ALL) {
            total = total.add(thresholds.seasonalPoints());
            appendNote(note, "季節性強");
        }
        if (moq != null && moq > thresholds.highMoq()) {
            total = total.add(thresholds.highMoqPoints());
            appendNote(note, "MOQ 偏高");
        }

        BigDecimal maxPenalty = BigDecimal.valueOf(FactorCode.INVENTORY_RISK.maxPenalty());
        BigDecimal penalty = total.min(maxPenalty);
        return new PenaltyContribution(FactorCode.INVENTORY_RISK, penalty,
                note.isEmpty() ? "效期／季節性／MOQ 皆未觸發" : note.toString());
    }

    private static void appendNote(StringBuilder note, String text) {
        if (!note.isEmpty()) {
            note.append("、");
        }
        note.append(text);
    }
}
