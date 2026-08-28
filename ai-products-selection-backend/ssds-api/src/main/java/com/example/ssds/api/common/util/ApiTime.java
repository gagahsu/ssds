package com.example.ssds.api.common.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 實體層 {@link Instant}（UTC）轉 API 回應用的 {@link OffsetDateTime}。
 *
 * <p>§8.1：「回應一律以 +08:00 呈現」。實體／資料庫端存 UTC 瞬時是刻意的
 * （見 {@code BaseAuditEntity} 的說明），但序列化給前端時必須轉換，
 * 否則 Jackson 對 {@code Instant} 預設輸出帶 {@code Z} 尾碼的 UTC 字串，違反 §8.1。
 * DTO 的時間欄位一律用 {@code OffsetDateTime}，由這裡統一轉換，不要在各 Service
 * 各自寫 {@code atZone(...)}。
 */
public final class ApiTime {

    private static final ZoneId API_ZONE = ZoneId.of("Asia/Taipei");

    private ApiTime() {
    }

    public static OffsetDateTime from(Instant instant) {
        return instant == null ? null : instant.atZone(API_ZONE).toOffsetDateTime();
    }
}
