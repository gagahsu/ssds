package com.example.ssds.infra.repository;

import com.example.ssds.core.domain.AlertStatus;
import com.example.ssds.core.domain.Severity;
import com.example.ssds.infra.entity.RiskAlert;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 風險示警（規格書 §7.2 risk_alert、FR-10）。
 *
 * <p>AC-10-2：預設清單排除 IGNORED，但可用篩選查回來 —— {@code JpaSpecificationExecutor}
 * 讓 §8.2 {@code GET /risks} 的 status／severity／type／categoryId 任意組合篩選在執行期組裝
 * （見 {@code RiskAlertSpecifications}），不必為每種組合寫衍生查詢方法。
 */
@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long>, JpaSpecificationExecutor<RiskAlert> {

    /** 預設清單：未忽略者。 */
    @EntityGraph(attributePaths = {"product", "product.category"})
    Page<RiskAlert> findByStatusNot(AlertStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.category"})
    Page<RiskAlert> findByStatusAndSeverity(AlertStatus status, Severity severity, Pageable pageable);

    List<RiskAlert> findByProductIdOrderByDetectedAtDesc(Long productId);

    /** 儀表板的高風險計數。 */
    long countByStatusAndSeverity(AlertStatus status, Severity severity);

    boolean existsByProductIdAndRiskTypeAndStatus(
            Long productId, String riskType, AlertStatus status);
}
