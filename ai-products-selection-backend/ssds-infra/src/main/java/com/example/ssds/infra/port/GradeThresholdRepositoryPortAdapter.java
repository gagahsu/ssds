package com.example.ssds.infra.port;

import com.example.ssds.core.domain.SceneType;
import com.example.ssds.core.port.GradeThresholdRepositoryPort;
import com.example.ssds.core.scoring.GradeThresholdSet;
import com.example.ssds.infra.entity.GradeThreshold;
import com.example.ssds.infra.repository.GradeThresholdRepository;
import org.springframework.stereotype.Component;

@Component
public class GradeThresholdRepositoryPortAdapter implements GradeThresholdRepositoryPort {

    private final GradeThresholdRepository gradeThresholdRepository;

    public GradeThresholdRepositoryPortAdapter(GradeThresholdRepository gradeThresholdRepository) {
        this.gradeThresholdRepository = gradeThresholdRepository;
    }

    @Override
    public GradeThresholdSet findThresholds(long weightVersionId, SceneType sceneType) {
        GradeThreshold threshold = gradeThresholdRepository
                .findByVersionIdAndSceneType(weightVersionId, sceneType)
                .orElseThrow(() -> new IllegalStateException(
                        "找不到分級門檻：weightVersionId=" + weightVersionId + ", sceneType=" + sceneType));
        return new GradeThresholdSet(sceneType, threshold.getGradeAMin(), threshold.getGradeBMin());
    }
}
