package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import java.math.BigDecimal;

/**
 * 扣分因子計算結果（規格書 §5.2.2）。以 0 至上限的正值儲存，負號只在 UI 呈現。
 *
 * <p>即使 {@code penaltyValue = 0} 仍必須產生本紀錄（AC-05-4：三個扣分項全列出）。
 */
public record PenaltyContribution(
        FactorCode factorCode,
        BigDecimal penaltyValue,
        String note
) {
    public PenaltyContribution {
        if (!factorCode.isPenalty()) {
            throw new IllegalArgumentException(factorCode + " 不是扣分因子");
        }
        if (penaltyValue.signum() < 0
                || penaltyValue.compareTo(BigDecimal.valueOf(factorCode.maxPenalty())) > 0) {
            throw new IllegalArgumentException(
                    factorCode + " 扣分需介於 0 與 " + factorCode.maxPenalty() + " 之間");
        }
    }
}
