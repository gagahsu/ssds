package com.example.ssds.api.dto;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.SceneType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 單一情境的六因子權重 + 該榜的 A/B 分級門檻（§7.2 weight_profile + grade_threshold）。
 * {@code weights} 只放六個加分因子（§5.2.2 扣分因子固定生效、不參與權重）。
 */
public record SceneWeightSet(
        @NotNull SceneType sceneType,
        @NotNull Map<FactorCode, BigDecimal> weights,
        @NotNull BigDecimal gradeAMin,
        @NotNull BigDecimal gradeBMin) {
}
