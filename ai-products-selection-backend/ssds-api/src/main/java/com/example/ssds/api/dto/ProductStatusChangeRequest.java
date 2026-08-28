package com.example.ssds.api.dto;

import com.example.ssds.core.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;

/** PATCH /products/{id}/status（§7.4 狀態機）。 */
public record ProductStatusChangeRequest(@NotNull ProductStatus status) {
}
