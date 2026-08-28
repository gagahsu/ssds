package com.example.ssds.api.risk;

import com.example.ssds.core.domain.AlertStatus;
import com.example.ssds.core.domain.Severity;
import com.example.ssds.infra.entity.RiskAlert;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;

/** §8.2 {@code GET /risks} 查詢參數轉 JPA Specification。 */
public final class RiskAlertSpecifications {

    private RiskAlertSpecifications() {
    }

    /** 未指定 status 時，AC-10-2 預設排除 IGNORED。 */
    public static Specification<RiskAlert> statusEquals(AlertStatus status) {
        if (status == null) {
            return (root, query, cb) -> cb.notEqual(root.get("status"), AlertStatus.IGNORED);
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<RiskAlert> severityEquals(Severity severity) {
        if (severity == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("severity"), severity);
    }

    public static Specification<RiskAlert> typeEquals(String riskType) {
        if (riskType == null || riskType.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("riskType"), riskType);
    }

    public static Specification<RiskAlert> categoryIdEquals(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("product").get("category").get("id"), categoryId);
    }

    @SafeVarargs
    public static Specification<RiskAlert> combine(Specification<RiskAlert>... specs) {
        return Specification.allOf(Arrays.stream(specs).filter(Objects::nonNull).toList());
    }
}
