package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.SceneType;
import java.math.BigDecimal;

/**
 * 單一榜（情境）的 A／B 分級門檻，隨 weight_version 一併版本化（規格書 §5.6、§7.2.5 grade_threshold）。
 *
 * <p>低於 {@code gradeBMin} 者為 C。
 */
public record GradeThresholdSet(SceneType sceneType, BigDecimal gradeAMin, BigDecimal gradeBMin) {
}
