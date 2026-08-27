package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.LogisticsCondition;
import java.math.BigDecimal;
import java.time.Month;
import java.util.Set;

/**
 * `LOGISTICS_RISK` 扣分（規格書 §5.2.2：冷鏈需求、易融化、易碎、材積異常，上限 10）。
 *
 * <p><b>點數表為 v3.0 佔位值</b>：規格書只給出上限（10）與黃金案例的單一結果
 * （夏季＋易融化 → 4），未定義各條件的確切點數——與品類前置天數、負評率門檻等其他
 * 「待客戶確認」欄位（附錄 A）同性質。以下點數經 golden case 驗算，實際值待
 * {@code risk_rule.threshold_json} 由 SYS_ADMIN 校準覆寫。
 */
public final class LogisticsRiskCalculator {

    private static final BigDecimal MELTABLE_SUMMER_POINTS = BigDecimal.valueOf(4);
    private static final BigDecimal COLD_CHAIN_POINTS = BigDecimal.valueOf(4);
    private static final BigDecimal FRAGILE_POINTS = BigDecimal.valueOf(3);
    private static final BigDecimal OVERSIZED_POINTS = BigDecimal.valueOf(3);
    private static final Set<Month> SUMMER_MONTHS =
            Set.of(Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER);

    private LogisticsRiskCalculator() {
    }

    public static PenaltyContribution calculate(Set<LogisticsCondition> conditions, Month evaluationMonth) {
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder note = new StringBuilder();

        if (conditions.contains(LogisticsCondition.MELTABLE) && SUMMER_MONTHS.contains(evaluationMonth)) {
            total = total.add(MELTABLE_SUMMER_POINTS);
            note.append("夏季高溫、易融化");
        }
        if (conditions.contains(LogisticsCondition.CHILLED) || conditions.contains(LogisticsCondition.FROZEN)) {
            total = total.add(COLD_CHAIN_POINTS);
            appendNote(note, "冷鏈需求");
        }
        if (conditions.contains(LogisticsCondition.FRAGILE)) {
            total = total.add(FRAGILE_POINTS);
            appendNote(note, "易碎");
        }
        if (conditions.contains(LogisticsCondition.OVERSIZED)) {
            total = total.add(OVERSIZED_POINTS);
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
