package com.example.ssds.core.domain;

/**
 * 熱度來源的資料粒度（規格書 §7.2.3 heat_source.granularity）。
 *
 * <p>{@code CATEGORY} 級來源（目前僅 Instagram）於合成時套用 0.5 的粒度折扣（§5.3.2）。
 */
public enum HeatSourceGranularity {
    KEYWORD,
    CATEGORY
}
