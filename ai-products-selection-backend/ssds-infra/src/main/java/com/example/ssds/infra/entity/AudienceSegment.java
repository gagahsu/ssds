package com.example.ssds.infra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * 去識別化的客群統計（規格書 §7.2 audience_segment、§5.2.4）。
 *
 * <p>只存客群代碼、名稱與價格帶，不含任何個人資料——`PRICE_FIT` 因子的計算完全在
 * 後端完成，只有結果百分位可送入 LLM（§6.8）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audience_segment")
public class AudienceSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audience_code", nullable = false, length = 24, unique = true)
    private String audienceCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "price_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMin;

    @Column(name = "price_max", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMax;

    @Column(length = 255)
    private String note;
}
