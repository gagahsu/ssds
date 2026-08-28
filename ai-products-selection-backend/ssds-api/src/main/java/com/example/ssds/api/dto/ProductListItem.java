package com.example.ssds.api.dto;

import com.example.ssds.core.domain.Grade;
import com.example.ssds.core.domain.LastScoringStatus;
import com.example.ssds.core.domain.ProductStatus;
import com.example.ssds.core.domain.SourcingStatus;
import com.example.ssds.core.domain.TrackType;
import java.math.BigDecimal;

/** FR-03-1 品項清單一列。§FR-03-1：B 軌不顯示成本/售價/毛利率/分數/分級，由前端依 trackType 自行隱藏。 */
public record ProductListItem(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        TrackType trackType,
        Long supplierId,
        String supplierName,
        BigDecimal cost,
        BigDecimal suggestedPrice,
        BigDecimal marginRate,
        BigDecimal latestScore,
        Grade latestGrade,
        ProductStatus status,
        SourcingStatus sourcingStatus,
        LastScoringStatus lastScoringStatus) {
}
