package com.example.ssds.api.dto;

import com.example.ssds.core.domain.Season;
import com.example.ssds.core.domain.SourcingStatus;
import com.example.ssds.core.domain.TrackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * FR-03-2 品項新增/編輯請求。
 *
 * <p>A/B 軌必填欄位不同（A：成本＋建議售價；B：至少一個關鍵字），此處只做通用欄位驗證，
 * 軌別專屬的必填規則由 {@code ProductService} 檢查，理由是要能回傳可讀的
 * {@code BusinessException}／{@code FieldError}，而不是讓 bean validation 用同一組
 * 註解硬套兩種軌別。
 */
public record ProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long categoryId,
        Long supplierId,
        BigDecimal cost,
        BigDecimal suggestedPrice,
        Integer moq,
        Season season,
        @NotNull TrackType trackType,
        SourcingStatus sourcingStatus,
        String logisticsCondition,
        Integer shelfLifeDays,
        BigDecimal idealTempMin,
        BigDecimal idealTempMax,
        List<Long> keywordIds) {
}
