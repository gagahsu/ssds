package com.example.ssds.core.scoring;

/** 同品類百分位的樣本退路分層（規格書 §5.3.1）。 */
public enum NormalizationTier {
    /** 同品類樣本 ≥ 10，直接使用 */
    OWN_CATEGORY,
    /** 樣本 3–9，與同一父品類下的兄弟品類合併計算 */
    SIBLING_MERGED,
    /** 樣本 < 3（含兄弟品類合併後仍不足），使用全品類百分位 */
    ALL_CATEGORY
}
