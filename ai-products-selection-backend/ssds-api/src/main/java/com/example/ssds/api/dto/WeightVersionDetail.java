package com.example.ssds.api.dto;

import com.example.ssds.core.domain.WeightVersionStatus;
import java.time.LocalDate;
import java.util.List;

/** FR-08 GET /weight-versions/{id}/profiles：該版本的四組情境權重與四榜門檻。 */
public record WeightVersionDetail(
        Long id,
        String versionNo,
        String name,
        WeightVersionStatus status,
        LocalDate effectiveFrom,
        boolean current,
        String changeNote,
        List<SceneWeightSet> scenes) {
}
