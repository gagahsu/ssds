package com.example.ssds.api.dto;

import com.example.ssds.core.domain.AlertStatus;
import com.example.ssds.core.domain.Severity;
import java.time.OffsetDateTime;

/** FR-10 風險示警清單一列。 */
public record RiskAlertListItem(
        Long id,
        Long productId,
        String productName,
        Long categoryId,
        String categoryName,
        String riskType,
        Severity severity,
        String triggerValue,
        AlertStatus status,
        String ignoreReason,
        OffsetDateTime detectedAt,
        OffsetDateTime handledAt,
        String handledByName) {
}
