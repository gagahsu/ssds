package com.example.ssds.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PATCH /risks/{id}/ignore（§FR-10：忽略理由必填）。 */
public record RiskIgnoreRequest(@NotBlank @Size(max = 300) String reason) {
}
