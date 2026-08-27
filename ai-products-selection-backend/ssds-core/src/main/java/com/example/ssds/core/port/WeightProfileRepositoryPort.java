package com.example.ssds.core.port;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.SceneType;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 情境權重組查詢（規格書 §7.2.5 weight_profile）。由 ssds-infra 實作。
 *
 * <p>回傳的六項權重必須加總為 1.000（AC-08-1，儲存時驗證，本介面假設已通過驗證）。
 */
public interface WeightProfileRepositoryPort {
    Map<FactorCode, BigDecimal> findWeights(long weightVersionId, SceneType sceneType);
}
