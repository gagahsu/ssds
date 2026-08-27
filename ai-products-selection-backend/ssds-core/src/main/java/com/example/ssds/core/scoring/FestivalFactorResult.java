package com.example.ssds.core.scoring;

import java.math.BigDecimal;

/**
 * @param effectiveFestivalCode 取最大值的節慶代碼；品項未關聯任何節慶時為 null（此時 rawValue = 0）
 */
public record FestivalFactorResult(BigDecimal rawValue, String effectiveFestivalCode) {
}
