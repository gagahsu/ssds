package com.example.ssds.api.weight;

import com.example.ssds.api.common.error.BusinessException;
import com.example.ssds.api.common.error.ErrorCode;
import com.example.ssds.api.common.util.ApiTime;
import com.example.ssds.api.dto.SceneWeightSet;
import com.example.ssds.api.dto.WeightVersionDetail;
import com.example.ssds.api.dto.WeightVersionSummary;
import com.example.ssds.api.dto.WeightVersionUpsertRequest;
import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.SceneType;
import com.example.ssds.core.domain.WeightVersionStatus;
import com.example.ssds.infra.entity.AppUser;
import com.example.ssds.infra.entity.GradeThreshold;
import com.example.ssds.infra.entity.WeightProfile;
import com.example.ssds.infra.entity.WeightVersion;
import com.example.ssds.infra.repository.AppUserRepository;
import com.example.ssds.infra.repository.GradeThresholdRepository;
import com.example.ssds.infra.repository.WeightProfileRepository;
import com.example.ssds.infra.repository.WeightVersionRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-08 情境權重組設定。§FR-08-3「核准生效並觸發全量重算」（{@code POST /{id}/approve}）本次未做，
 * 理由見 docs/module-tasks.md：重算需要非同步任務追蹤基礎設施，屬於評分批次觸發時機表的範圍。
 */
@Service
public class WeightVersionService {

    /** §5.2.2：本表只放六個加分因子，扣分因子固定生效、不參與權重。 */
    private static final Set<FactorCode> BONUS_FACTORS = Arrays.stream(FactorCode.values())
            .filter(f -> !f.isPenalty())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    private final WeightVersionRepository weightVersionRepository;
    private final WeightProfileRepository weightProfileRepository;
    private final GradeThresholdRepository gradeThresholdRepository;
    private final AppUserRepository appUserRepository;

    public WeightVersionService(
            WeightVersionRepository weightVersionRepository,
            WeightProfileRepository weightProfileRepository,
            GradeThresholdRepository gradeThresholdRepository,
            AppUserRepository appUserRepository) {
        this.weightVersionRepository = weightVersionRepository;
        this.weightProfileRepository = weightProfileRepository;
        this.gradeThresholdRepository = gradeThresholdRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<WeightVersionSummary> list() {
        return weightVersionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public WeightVersionDetail active() {
        WeightVersion version = weightVersionRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "目前沒有生效中的權重版本"));
        return toDetail(version);
    }

    @Transactional(readOnly = true)
    public WeightVersionDetail profiles(Long id) {
        WeightVersion version = weightVersionRepository.findWithProfilesById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toDetail(version);
    }

    @Transactional
    public WeightVersionDetail create(WeightVersionUpsertRequest request, Long actingUserId) {
        validateScenes(request.scenes());
        WeightVersion version = WeightVersion.builder()
                .versionNo(nextVersionNo())
                .name(request.name())
                .changeNote(request.changeNote())
                .effectiveFrom(request.effectiveFrom())
                .status(WeightVersionStatus.DRAFT)
                .createdBy(actingUserId == null ? null : appUserRepository.getReferenceById(actingUserId))
                .build();
        version = weightVersionRepository.save(version);
        applyScenes(version, request.scenes());
        return toDetail(weightVersionRepository.findWithProfilesById(version.getId()).orElseThrow());
    }

    @Transactional
    public WeightVersionDetail update(Long id, WeightVersionUpsertRequest request) {
        WeightVersion version = weightVersionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!version.isEditable()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "已核准的權重版本不可編輯，請建立新版本");
        }
        validateScenes(request.scenes());

        version.setName(request.name());
        version.setChangeNote(request.changeNote());
        version.setEffectiveFrom(request.effectiveFrom());

        weightProfileRepository.deleteAll(weightProfileRepository.findByVersionId(id));
        version.getProfiles().clear();
        for (SceneType scene : SceneType.values()) {
            gradeThresholdRepository.findByVersionIdAndSceneType(id, scene).ifPresent(gradeThresholdRepository::delete);
        }
        applyScenes(version, request.scenes());

        return toDetail(weightVersionRepository.findWithProfilesById(id).orElseThrow());
    }

    /**
     * 存完後把新列補進 {@code version.getProfiles()}：{@code save()} 不會自動同步
     * 這個集合，之後 {@link #toDetail} 若拿到的是同一個受管理的 {@code WeightVersion}
     * 實例（JPA 一級快取），重新查詢也不會重新載入，會看到空集合。
     */
    private void applyScenes(WeightVersion version, List<SceneWeightSet> scenes) {
        for (SceneWeightSet scene : scenes) {
            scene.weights().forEach((factor, weight) -> {
                WeightProfile saved = weightProfileRepository.save(WeightProfile.builder()
                        .version(version)
                        .sceneType(scene.sceneType())
                        .factorCode(factor)
                        .weight(weight)
                        .build());
                version.getProfiles().add(saved);
            });
            gradeThresholdRepository.save(GradeThreshold.builder()
                    .version(version)
                    .sceneType(scene.sceneType())
                    .gradeAMin(scene.gradeAMin())
                    .gradeBMin(scene.gradeBMin())
                    .build());
        }
    }

    /** AC-08-1：每個情境的六因子權重加總須為 1.000，且不得混入扣分因子。 */
    private void validateScenes(List<SceneWeightSet> scenes) {
        Set<SceneType> seen = new LinkedHashSet<>();
        for (SceneWeightSet scene : scenes) {
            if (!seen.add(scene.sceneType())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "同一情境不可重複提供權重組: " + scene.sceneType());
            }
            if (!scene.weights().keySet().equals(BONUS_FACTORS)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "情境 " + scene.sceneType() + " 必須提供六個加分因子的權重，且不可包含扣分因子");
            }
            BigDecimal sum = scene.weights().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(BigDecimal.ONE) != 0) {
                throw new BusinessException(ErrorCode.WEIGHT_SUM_INVALID,
                        "情境 " + scene.sceneType() + " 權重加總為 " + sum + "，須等於 1.000");
            }
            if (scene.gradeAMin().compareTo(scene.gradeBMin()) <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "gradeAMin 必須大於 gradeBMin");
            }
        }
        if (seen.size() != SceneType.values().length) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "必須提供全部四種情境的權重組");
        }
    }

    private String nextVersionNo() {
        long count = weightVersionRepository.count();
        return "v" + (count + 1);
    }

    private WeightVersionSummary toSummary(WeightVersion v) {
        return new WeightVersionSummary(
                v.getId(), v.getVersionNo(), v.getName(), v.getStatus(), v.getEffectiveFrom(),
                v.isCurrent(), v.getChangeNote(), ApiTime.from(v.getCreatedAt()), ApiTime.from(v.getApprovedAt()));
    }

    private WeightVersionDetail toDetail(WeightVersion v) {
        List<SceneWeightSet> scenes = Arrays.stream(SceneType.values())
                .map(scene -> {
                    var weights = v.getProfiles().stream()
                            .filter(p -> p.getSceneType() == scene)
                            .collect(java.util.stream.Collectors.toMap(
                                    WeightProfile::getFactorCode, WeightProfile::getWeight,
                                    (a, b) -> a, LinkedHashMap::new));
                    var threshold = gradeThresholdRepository.findByVersionIdAndSceneType(v.getId(), scene).orElse(null);
                    return new SceneWeightSet(
                            scene, weights,
                            threshold == null ? null : threshold.getGradeAMin(),
                            threshold == null ? null : threshold.getGradeBMin());
                })
                .toList();
        return new WeightVersionDetail(
                v.getId(), v.getVersionNo(), v.getName(), v.getStatus(), v.getEffectiveFrom(),
                v.isCurrent(), v.getChangeNote(), scenes);
    }
}
