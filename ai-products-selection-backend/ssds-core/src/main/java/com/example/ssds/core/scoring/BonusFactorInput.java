package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import java.math.BigDecimal;

/**
 * 單一加分因子的輸入（規格書 §5.2.1、§5.7）。
 *
 * <p>{@code dataAvailable = false} 時該因子不參與加權，其權重由
 * {@link WeightAllocator} 分攤給其餘因子；{@code imputed = true} 時仍正常加權，
 * 但由 {@link ConfidenceCalculator} 另行扣分（§5.7 的欄位分工，兩者互斥）。
 *
 * @param rawValue        原始值；{@code dataAvailable = false} 時可為 null
 * @param normalizedValue 同品類百分位（0–100）；{@code dataAvailable = false} 時可為 null
 */
public record BonusFactorInput(
        FactorCode factorCode,
        BigDecimal rawValue,
        BigDecimal normalizedValue,
        boolean dataAvailable,
        boolean imputed,
        String note
) {
    public BonusFactorInput {
        if (factorCode.isPenalty()) {
            throw new IllegalArgumentException(factorCode + " 是扣分因子，不可作為 BonusFactorInput");
        }
        if (dataAvailable && imputed) {
            // 兩者不互斥於此處判斷失敗——data_available=true 時 imputed 可以是 true 或 false，
            // 唯一禁止的組合是 data_available=false 且 imputed=true（§5.7）。
        }
        if (!dataAvailable && imputed) {
            throw new IllegalArgumentException("data_available = false 時 imputed 必為 false（§5.7）");
        }
    }

    public static BonusFactorInput unavailable(FactorCode factorCode, String note) {
        return new BonusFactorInput(factorCode, null, null, false, false, note);
    }
}
