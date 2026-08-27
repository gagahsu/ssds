package com.example.ssds.infra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * 品類層級的預設適溫區間（規格書 §7.2 category_climate_profile、§FR-17-2）。
 *
 * <p>品項未填 {@code ideal_temp_min/max} 時沿用此值；兩者皆無時 `CLIMATE` 因子標為
 * 無資料（AC-17-5）。與 {@link Category} 1:1 共用主鍵，同 {@link CategoryLeadTime}。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_climate_profile")
public class CategoryClimateProfile {

    @Id
    @Column(name = "category_id")
    private Long categoryId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "ideal_temp_min", nullable = false, precision = 4, scale = 1)
    private BigDecimal idealTempMin;

    @Column(name = "ideal_temp_max", nullable = false, precision = 4, scale = 1)
    private BigDecimal idealTempMax;

    /** 適配度衰減的容忍範圍，預設 12.0°C，可由 SYS_ADMIN 調整（§FR-17-2）。 */
    @Column(nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal tolerance = BigDecimal.valueOf(12.0);
}
