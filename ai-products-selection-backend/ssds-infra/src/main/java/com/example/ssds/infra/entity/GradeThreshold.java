package com.example.ssds.infra.entity;

import com.example.ssds.core.domain.SceneType;
import com.example.ssds.infra.entity.id.GradeThresholdId;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * 四榜的 A／B 分級門檻，隨權重版本一併版本化（規格書 §7.2 grade_threshold、§5.6）。
 *
 * <p>低於 {@link #gradeBMin} 者為 C。門檻與權重同屬評分規則，一起版本化才能重現
 * 當時的分級（v3.0 新增，見規格書 §7.2.5 的變更說明）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "grade_threshold")
@IdClass(GradeThresholdId.class)
public class GradeThreshold {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private WeightVersion version;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "scene_type", nullable = false, length = 24)
    private SceneType sceneType;

    @Column(name = "grade_a_min", nullable = false, precision = 5, scale = 2)
    private BigDecimal gradeAMin;

    @Column(name = "grade_b_min", nullable = false, precision = 5, scale = 2)
    private BigDecimal gradeBMin;
}
