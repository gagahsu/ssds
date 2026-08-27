package com.example.ssds.core.port;

import com.example.ssds.core.scoring.HeatSourceContribution;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 熱度來源與合成資料查詢（規格書 §7.2.3 heat_reading、heat_composite_daily）。由 ssds-infra 實作。
 */
public interface HeatCompositeRepositoryPort {
    List<HeatSourceContribution> findSourceContributions(long keywordId, LocalDate statDate);

    /** @return keyword 當日合成熱度（{@code composite_value}）；不存在時為空 */
    Optional<java.math.BigDecimal> findCompositeValue(long keywordId, LocalDate statDate);
}
