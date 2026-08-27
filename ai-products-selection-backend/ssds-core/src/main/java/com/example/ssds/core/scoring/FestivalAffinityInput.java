package com.example.ssds.core.scoring;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 品項×節慶關聯度，農曆已換算為國曆日期（規格書 §7.2.10 item_festival_affinity、festival_calendar）。 */
public record FestivalAffinityInput(String festivalCode, LocalDate festivalDate, BigDecimal affinity) {
}
