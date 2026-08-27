package com.example.ssds.core.port;

import com.example.ssds.core.scoring.FestivalAffinityInput;
import java.util.List;

/** 品項×節慶關聯度查詢，含農曆換算後的節慶日期（規格書 §7.2.10）。由 ssds-infra 實作。 */
public interface FestivalAffinityRepositoryPort {
    List<FestivalAffinityInput> findAffinities(long productId, int year);

    /** 品類前置天數（§FR-17-1；與 §FR-16 時效落差共用同一份，見 category_lead_time）。 */
    int findLeadTimeDays(long categoryId);
}
