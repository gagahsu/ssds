package com.example.ssds.core.scoring;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.Grade;
import java.math.BigDecimal;

/**
 * 分級規則（規格書 §5.6）。
 *
 * <p>硬規則：扣分小計 ≥ {@link FactorCode#PENALTY_GRADE_SUPPRESS_THRESHOLD}（20）者，
 * 分級最高只給 B（{@code PENALTY_CAP}）——避免高分掩蓋致命風險，評論風險單項扣分上限
 * 即為 20，因此食安類負評必然壓級，這是刻意設計。
 */
public final class GradeClassifier {

    private GradeClassifier() {
    }

    public static Grade classify(
            BigDecimal finalScore, GradeThresholdSet thresholds, BigDecimal penaltySubtotal) {
        Grade grade;
        if (finalScore.compareTo(thresholds.gradeAMin()) >= 0) {
            grade = Grade.A;
        } else if (finalScore.compareTo(thresholds.gradeBMin()) >= 0) {
            grade = Grade.B;
        } else {
            grade = Grade.C;
        }

        boolean penaltyCapped =
                penaltySubtotal.compareTo(BigDecimal.valueOf(FactorCode.PENALTY_GRADE_SUPPRESS_THRESHOLD)) >= 0;
        if (penaltyCapped && grade == Grade.A) {
            return Grade.B;
        }
        return grade;
    }
}
