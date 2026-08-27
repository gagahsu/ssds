package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import java.math.BigDecimal;

/**
 * 加分因子計算結果，直接對應 {@code score_factor} 一列（規格書 §7.2.6）。
 *
 * @param weight       分攤後的有效權重；{@code dataAvailable = false} 時為 0
 * @param contribution weight × normalizedValue；{@code dataAvailable = false} 時為 0
 */
public record BonusFactorContribution(
        FactorCode factorCode,
        BigDecimal rawValue,
        BigDecimal normalizedValue,
        BigDecimal weight,
        BigDecimal contribution,
        boolean dataAvailable,
        boolean imputed,
        String note
) {
}
