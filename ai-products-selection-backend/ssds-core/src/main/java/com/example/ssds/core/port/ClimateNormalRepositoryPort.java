package com.example.ssds.core.port;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 歷史同期氣候統計查詢（規格書 §7.2.10 climate_normal）。由 ssds-infra 實作。
 *
 * <p>僅供選品評分（月均溫，穩定可用），不得混用短期天氣預報（AC-17-4）。
 */
public interface ClimateNormalRepositoryPort {
    Optional<BigDecimal> findAvgTemp(String regionCode, int month);

    /** 品項未填適溫區間時的品類預設（category_climate_profile）；兩者皆無時 CLIMATE 因子標為無資料。 */
    Optional<IdealTempRange> findCategoryIdealTempRange(long categoryId);

    record IdealTempRange(BigDecimal min, BigDecimal max) {
    }
}
