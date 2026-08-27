package com.example.ssds.core.port;

import com.example.ssds.core.scoring.AudienceSegmentShare;
import java.util.List;

/**
 * 品類客群組成查詢（規格書 §7.2.2 category_audience_mix × audience_segment）。由 ssds-infra 實作。
 *
 * <p>只回傳去識別化的統計值，不含任何個人資料（§5.2.4）。空清單表示客戶未提供客群價格帶，
 * `PRICE_FIT` 因子應標為無資料（R-20 降級路徑）。
 */
public interface AudienceMixRepositoryPort {
    List<AudienceSegmentShare> findMixForCategory(long categoryId);
}
