package com.example.ssds.infra.entity;

import com.example.ssds.infra.entity.id.CategoryAudienceMixId;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * 品類的客群組成（規格書 §7.2 category_audience_mix、§5.2.4）。
 *
 * <p>同一 {@code category} 的 {@link #share} 加總須為 1.000（應用層驗證，跨列條件
 * SQL CHECK 表達不了，同 {@link WeightProfile} 的權重加總）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_audience_mix")
@IdClass(CategoryAudienceMixId.class)
public class CategoryAudienceMix {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audience_id", nullable = false)
    private AudienceSegment audience;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal share;
}
