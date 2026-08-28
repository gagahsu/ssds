package com.example.ssds.api.dto;

/** FR-03-2 例外條件：同類別同名品項不阻擋儲存，改以 {@code duplicateNameWarning} 回傳警告供前端提示。 */
public record ProductSaveResult(ProductDetail product, boolean duplicateNameWarning) {
}
