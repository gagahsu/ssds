package com.example.ssds.infra.entity;

import com.example.ssds.core.domain.HeatStage;
import com.example.ssds.infra.entity.id.HeatCompositeDailyId;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 多來源合成後的每日熱度（規格書 §7.2 heat_composite_daily、§5.3.2）。
 *
 * <p>是 §FR-06 曲線、§5.3.3 斜率與 §5.8 階段判定的<b>唯一</b>資料來源；
 * {@link #appliedWeights} 記錄本次實際採用的各來源合成權重，供事後追溯（AC-14-5）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "heat_composite_daily")
@IdClass(HeatCompositeDailyId.class)
public class HeatCompositeDaily {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keyword_id", nullable = false)
    private TrendKeyword keyword;

    @Id
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "composite_value", nullable = false, precision = 6, scale = 2)
    private BigDecimal compositeValue;

    @Column(name = "slope_7d", precision = 8, scale = 4)
    private BigDecimal slope7d;

    @Column(name = "slope_30d", precision = 8, scale = 4)
    private BigDecimal slope30d;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HeatStage stage;

    @Column(name = "stage_weeks", nullable = false)
    @Builder.Default
    private short stageWeeks = 0;

    @Column(name = "estimated_lifespan_days")
    private Integer estimatedLifespanDays;

    /** 本次實際採用的各來源合成權重（§5.3.2 effective_weight）。 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applied_weights", nullable = false, columnDefinition = "jsonb")
    private String appliedWeights;

    /** 7 日與 30 日背離（可能見頂），不直接影響分數，只供 UI 標示（§5.3.3）。 */
    @Column(name = "divergence_flag", nullable = false)
    @Builder.Default
    private boolean divergenceFlag = false;

    /** 熱度量級未達下限（§5.2.1-a），TREND 因子仍視為有資料，只是強制低分。 */
    @Column(name = "volume_below_floor", nullable = false)
    @Builder.Default
    private boolean volumeBelowFloor = false;
}
