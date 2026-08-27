package com.example.ssds.core.scoring;

import java.util.List;

/**
 * 分數信心度（規格書 §5.9）。
 *
 * <pre>confidence = clamp(100 − Σ(扣分項), 0, 100)</pre>
 */
public final class ConfidenceCalculator {

    private ConfidenceCalculator() {
    }

    public static int calculate(List<ConfidencePenaltyReason> reasons) {
        int total = 0;
        for (ConfidencePenaltyReason reason : reasons) {
            total += reason.penalty();
        }
        int confidence = 100 - total;
        if (confidence < 0) {
            return 0;
        }
        return Math.min(confidence, 100);
    }
}
