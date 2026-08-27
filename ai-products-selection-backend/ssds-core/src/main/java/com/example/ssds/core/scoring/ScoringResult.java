package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.Grade;
import java.math.BigDecimal;
import java.util.List;

/**
 * 單筆 {@code product_score} 的完整計算結果（規格書 §5.5、§7.2.6）。
 *
 * <p>{@code sufficientData = false} 時（六項加分因子缺 4 項以上，§5.7）
 * {@code bonusSubtotal}／{@code finalScore}／{@code grade} 皆為 null，
 * 但扣分仍照常計算並回傳，供風險示警獨立運作。
 */
public record ScoringResult(
        boolean sufficientData,
        BigDecimal bonusSubtotal,
        BigDecimal penaltySubtotal,
        BigDecimal finalScore,
        Grade grade,
        List<BonusFactorContribution> factorContributions,
        List<PenaltyContribution> penaltyContributions
) {
}
