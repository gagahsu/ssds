package com.example.ssds.infra.repository;

import com.example.ssds.core.domain.SceneType;
import com.example.ssds.infra.entity.GradeThreshold;
import com.example.ssds.infra.entity.id.GradeThresholdId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 分級門檻，隨權重版本一併版本化（規格書 §7.2 grade_threshold、§5.6）。 */
@Repository
public interface GradeThresholdRepository extends JpaRepository<GradeThreshold, GradeThresholdId> {

    Optional<GradeThreshold> findByVersionIdAndSceneType(Long versionId, SceneType sceneType);
}
