package com.example.ssds.core.port;

import com.example.ssds.core.domain.SceneType;
import com.example.ssds.core.scoring.GradeThresholdSet;

/** 分級門檻查詢（規格書 §7.2.5 grade_threshold）。由 ssds-infra 實作。 */
public interface GradeThresholdRepositoryPort {
    GradeThresholdSet findThresholds(long weightVersionId, SceneType sceneType);
}
