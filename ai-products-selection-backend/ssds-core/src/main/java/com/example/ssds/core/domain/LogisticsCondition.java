package com.example.ssds.core.domain;

/** 物流條件（規格書 §7.2.2 product.logistics_condition，DB 端以 SET 儲存）。 */
public enum LogisticsCondition {
    NORMAL,
    CHILLED,
    FROZEN,
    FRAGILE,
    MELTABLE,
    OVERSIZED
}
