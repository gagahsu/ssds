package com.example.ssds.api.dto;

import com.example.ssds.core.domain.LastScoringStatus;
import com.example.ssds.core.domain.ProductStatus;
import com.example.ssds.core.domain.Season;
import com.example.ssds.core.domain.SourcingStatus;
import com.example.ssds.core.domain.TrackType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** FR-05 品項詳情的品項主檔部分（評分/AI 洞察區塊由其他端點提供）。 */
public record ProductDetail(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        Long supplierId,
        String supplierName,
        BigDecimal cost,
        BigDecimal suggestedPrice,
        BigDecimal marginRate,
        Integer moq,
        Season season,
        ProductStatus status,
        String rejectReason,
        LocalDate listedAt,
        TrackType trackType,
        SourcingStatus sourcingStatus,
        String logisticsCondition,
        Integer shelfLifeDays,
        BigDecimal idealTempMin,
        BigDecimal idealTempMax,
        LastScoringStatus lastScoringStatus,
        OffsetDateTime lastScoringAttemptedAt,
        List<Long> keywordIds,
        List<String> keywords,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
