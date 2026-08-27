package com.example.ssds.core.scoring;

/**
 * 信心度扣分項目（規格書 §5.9）。起算值 100，下限 0。
 *
 * <p>{@code PER_MISSING_FACTOR}／{@code PER_IMPUTED_FACTOR}／{@code PER_DEGRADED_HEAT_SOURCE}
 * 三項可在同一筆分數中重複出現多次（每個因子／來源各算一次），其餘四項最多各算一次。
 */
public enum ConfidencePenaltyReason {
    /** 同品類樣本 < 10（含兄弟品類與全品類退路），§5.3.1 */
    LOW_CATEGORY_SAMPLE(20),
    /** 熱度來源降級或不可用，每個 −10（可重複） */
    PER_DEGRADED_HEAT_SOURCE(10),
    /** 加分因子 data_available = false，每個 −8（可重複） */
    PER_MISSING_FACTOR(8),
    /** 加分因子 is_imputed = true，每個 −4（可重複） */
    PER_IMPUTED_FACTOR(4),
    /** 情境判定 ai_confidence < 0.7 */
    LOW_SCENE_CONFIDENCE(10),
    /** 人工標記來源僅單人標記 */
    SINGLE_TAGGER(5),
    /** 熱度歷史不滿 30 日（slope_30d 以短區間推估） */
    SHORT_HEAT_HISTORY(5);

    private final int penalty;

    ConfidencePenaltyReason(int penalty) {
        this.penalty = penalty;
    }

    public int penalty() {
        return penalty;
    }
}
