package com.example.ssds.infra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 扣分規則與示警門檻，由 SYS_ADMIN 維護（規格書 §7.2 risk_rule、§5.2.2、§FR-10-1）。
 *
 * <p>{@link #category} 非 null 時為該品類的覆寫值，null 為全域預設。三類扣分規則
 * （{@code REVIEW_RISK}／{@code LOGISTICS_RISK}／{@code INVENTORY_RISK}）不可停用，
 * 由應用層強制，本表的 {@link #enabled} 只對 {@code HEAT_CRASH} 等示警類規則有意義。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "risk_rule")
public class RiskRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 32)
    private String ruleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** 各規則的門檻參數，如負評率 0.15、slope 門檻 -0.40（§7.2）。 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "threshold_json", nullable = false, columnDefinition = "jsonb")
    private String thresholdJson;

    @Column(name = "max_penalty", precision = 4, scale = 1)
    private BigDecimal maxPenalty;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
