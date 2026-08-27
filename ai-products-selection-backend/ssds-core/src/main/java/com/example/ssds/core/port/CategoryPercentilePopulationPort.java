package com.example.ssds.core.port;

import com.example.ssds.core.domain.FactorCode;
import java.math.BigDecimal;
import java.util.List;

/**
 * 同品類百分位正規化所需的母體查詢（規格書 §5.3.1）。由 ssds-infra 實作。
 *
 * <p>三段退路對應三個方法：≥10 用 own；3–9 用 sibling merged；&lt;3 用 all。
 * 呼叫端（{@link com.example.ssds.core.scoring.PercentileNormalizer}）依 own 的樣本數決定用哪一個。
 *
 * @implSpec {@code period} 為 ISO 週字串（如 {@code 2026W30}，見
 *         {@code product_score.period}），不是日期——同品類母體是「同一期已算出的
 *         其他品項原始值」，跟 {@link com.example.ssds.core.scoring.ScoringEngine}
 *         同一批評分共用同一個 period。
 */
public interface CategoryPercentilePopulationPort {
    List<BigDecimal> findOwnCategoryValues(FactorCode factorCode, long categoryId, String period);

    /** @param categoryId 品項自己的品類；實作端負責找出其父品類下的所有兄弟品類再合併 */
    List<BigDecimal> findSiblingMergedValues(FactorCode factorCode, long categoryId, String period);

    List<BigDecimal> findAllCategoryValues(FactorCode factorCode, String period);
}
