package com.example.ssds.core.domain;

/**
 * 最近一次評分嘗試的技術結果（規格書 §5.7 落地機制、§7.2.2 product.last_scoring_status）。
 *
 * <p>與 {@link ProductStatus} 的採購業務狀態機分開維護：後者是「品項被人判定觀察中」，
 * 前者是「系統根本沒算出分數」，兩者混在一起會分不清楚。
 */
public enum LastScoringStatus {
    /** 本次評分嘗試成功產生分數。 */
    SCORED,
    /** 六項加分因子缺 4 項以上，未產生分數（§5.7）。 */
    INSUFFICIENT_DATA
}
