package com.example.ssds.infra.repository;

import com.example.ssds.infra.entity.RiskRule;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 扣分規則門檻，由 SYS_ADMIN 維護（規格書 §7.2 risk_rule）。 */
@Repository
public interface RiskRuleRepository extends JpaRepository<RiskRule, Long> {

    Optional<RiskRule> findByRuleCodeAndCategoryId(String ruleCode, Long categoryId);

    /** 全域預設值（category_id 為 null）。 */
    Optional<RiskRule> findByRuleCodeAndCategoryIsNull(String ruleCode);
}
