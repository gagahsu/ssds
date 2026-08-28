package com.example.ssds.api.dto;

import com.example.ssds.core.domain.WeightVersionStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** FR-08 權重版本清單一列。 */
public record WeightVersionSummary(
        Long id,
        String versionNo,
        String name,
        WeightVersionStatus status,
        LocalDate effectiveFrom,
        boolean current,
        String changeNote,
        OffsetDateTime createdAt,
        OffsetDateTime approvedAt) {
}
