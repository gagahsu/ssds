package com.example.ssds.core.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ssds.core.domain.Grade;
import com.example.ssds.core.domain.SceneType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 規格書 §5.6：分級門檻 + PENALTY_CAP 硬規則（扣分 ≥ 20 時最高只給 B）。 */
class GradeClassifierTest {

    private static final GradeThresholdSet VIRAL =
            new GradeThresholdSet(SceneType.VIRAL, BigDecimal.valueOf(85), BigDecimal.valueOf(70));

    @Test
    void aboveAMin_isGradeA() {
        Grade grade = GradeClassifier.classify(BigDecimal.valueOf(90), VIRAL, BigDecimal.ZERO);
        assertThat(grade).isEqualTo(Grade.A);
    }

    @Test
    void penaltyCap_suppressesAtoB() {
        // 分數達 A 門檻，但扣分小計 20（達 PENALTY_GRADE_SUPPRESS_THRESHOLD）→ 強制降為 B
        Grade grade = GradeClassifier.classify(BigDecimal.valueOf(90), VIRAL, BigDecimal.valueOf(20));
        assertThat(grade).isEqualTo(Grade.B);
    }

    @Test
    void penaltyCap_doesNotAffectAlreadyBOrC() {
        Grade grade = GradeClassifier.classify(BigDecimal.valueOf(50), VIRAL, BigDecimal.valueOf(20));
        assertThat(grade).isEqualTo(Grade.C);
    }

    @Test
    void belowBMin_isGradeC() {
        Grade grade = GradeClassifier.classify(BigDecimal.valueOf(60), VIRAL, BigDecimal.ZERO);
        assertThat(grade).isEqualTo(Grade.C);
    }
}
