package com.example.ssds.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** POST/PUT /weight-versions（草稿建立/編輯，§FR-08，僅 BUYER_LEAD）。 */
public record WeightVersionUpsertRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 512) String changeNote,
        LocalDate effectiveFrom,
        @NotEmpty List<SceneWeightSet> scenes) {
}
