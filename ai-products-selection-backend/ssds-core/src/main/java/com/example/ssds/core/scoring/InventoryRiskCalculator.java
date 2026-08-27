package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.Season;
import java.math.BigDecimal;

/**
 * `INVENTORY_RISK` 扣分（規格書 §5.2.2：效期短、季節性強、最小訂購量偏高，上限 10）。
 *
 * <p><b>門檻為 v3.0 佔位值</b>，理由同 {@link LogisticsRiskCalculator}——規格書只給出上限與
 * 黃金案例的單一結果（效期 180 天、MOQ 200、季節 ALL → 皆未觸發），未定義確切門檻。
 * 以下門檻經 golden case 驗算，實際值待 SYS_ADMIN 校準覆寫。
 */
public final class InventoryRiskCalculator {

    private static final int SHORT_SHELF_LIFE_DAYS = 60;
    private static final int HIGH_MOQ = 300;
    private static final BigDecimal SHORT_SHELF_LIFE_POINTS = BigDecimal.valueOf(4);
    private static final BigDecimal SEASONAL_POINTS = BigDecimal.valueOf(3);
    private static final BigDecimal HIGH_MOQ_POINTS = BigDecimal.valueOf(3);

    private InventoryRiskCalculator() {
    }

    public static PenaltyContribution calculate(Integer shelfLifeDays, Season season, Integer moq) {
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder note = new StringBuilder();

        if (shelfLifeDays != null && shelfLifeDays < SHORT_SHELF_LIFE_DAYS) {
            total = total.add(SHORT_SHELF_LIFE_POINTS);
            note.append("效期短");
        }
        if (season != Season.ALL) {
            total = total.add(SEASONAL_POINTS);
            appendNote(note, "季節性強");
        }
        if (moq != null && moq > HIGH_MOQ) {
            total = total.add(HIGH_MOQ_POINTS);
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
