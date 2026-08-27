package com.example.ssds.core.port;

import com.example.ssds.core.domain.SceneType;
import com.example.ssds.core.scoring.ScoringResult;
import java.time.OffsetDateTime;

/**
 * 評分結果寫入（規格書 §7.2.6 product_score／score_factor、§5.10 計算時機）。由 ssds-infra 實作。
 *
 * <p>每次評分產生新紀錄，保留歷史不覆寫；同 {@code (product_id, period, scene_type)} 重複評分時，
 * 實作端須負責把舊紀錄的 {@code is_active} 改為 false，只有本次寫入的新紀錄為 true。
 */
public interface ProductScoreRepositoryPort {
    /**
     * @param result   {@link ScoringResult#sufficientData()} 必須為 true——資料不足
     *                 （§5.7）時不產生 product_score 列，呼叫端負責在呼叫前過濾，
     *                 不應把「無法評分」寫成一筆 grade 為 null 的紀錄
     * @param confidence 0–100，由 {@link com.example.ssds.core.scoring.ConfidenceCalculator} 算出
     */
    void save(
            long productId, String period, SceneType sceneType, boolean isPrimary,
            long weightVersionId, ScoringResult result, int confidence, OffsetDateTime calculatedAt);
}
