package com.example.ssds.infra.entity.id;

import com.example.ssds.core.domain.SceneType;
import java.io.Serializable;
import lombok.*;

/** grade_threshold 的複合主鍵（version_id, scene_type）。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GradeThresholdId implements Serializable {

    private Long version;
    private SceneType sceneType;
}
