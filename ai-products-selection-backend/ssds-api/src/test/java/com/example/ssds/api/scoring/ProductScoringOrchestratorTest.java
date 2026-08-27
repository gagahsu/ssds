package com.example.ssds.api.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.ProductStatus;
import com.example.ssds.core.domain.SceneType;
import com.example.ssds.core.domain.Season;
import com.example.ssds.core.domain.TrackType;
import com.example.ssds.core.port.AudienceMixRepositoryPort;
import com.example.ssds.core.port.ClimateNormalRepositoryPort;
import com.example.ssds.core.port.FestivalAffinityRepositoryPort;
import com.example.ssds.core.port.GradeThresholdRepositoryPort;
import com.example.ssds.core.port.ProductScoreRepositoryPort;
import com.example.ssds.core.port.RiskRuleConfig;
import com.example.ssds.core.port.RiskRuleRepositoryPort;
import com.example.ssds.core.port.WeightProfileRepositoryPort;
import com.example.ssds.core.scoring.AudienceSegmentShare;
import com.example.ssds.core.scoring.BonusFactorContribution;
import com.example.ssds.core.scoring.GradeThresholdSet;
import com.example.ssds.core.scoring.ScoringResult;
import com.example.ssds.infra.entity.Category;
import com.example.ssds.infra.entity.Product;
import com.example.ssds.infra.entity.WeightVersion;
import com.example.ssds.infra.repository.HeatCompositeDailyRepository;
import com.example.ssds.infra.repository.ProductRepository;
import com.example.ssds.infra.repository.ProductReviewRepository;
import com.example.ssds.infra.repository.RiskAlertRepository;
import com.example.ssds.infra.repository.SalesRecordRepository;
import com.example.ssds.infra.repository.WeightVersionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 涵蓋 §5.10 兩段式批次順序（同品類母體須包含整批其他品項，不能逐品項各算各的）與
 * §5.7 資料不足時不寫入 product_score。其餘因子細節有各自 calculator 的測試把關
 * （見 ssds-core），這裡只驗證編排本身接得對。
 */
@ExtendWith(MockitoExtension.class)
class ProductScoringOrchestratorTest {

    @Mock private ProductRepository productRepository;
    @Mock private WeightVersionRepository weightVersionRepository;
    @Mock private WeightProfileRepositoryPort weightProfileRepositoryPort;
    @Mock private GradeThresholdRepositoryPort gradeThresholdRepositoryPort;
    @Mock private RiskRuleRepositoryPort riskRuleRepositoryPort;
    @Mock private ClimateNormalRepositoryPort climateNormalRepositoryPort;
    @Mock private AudienceMixRepositoryPort audienceMixRepositoryPort;
    @Mock private FestivalAffinityRepositoryPort festivalAffinityRepositoryPort;
    @Mock private ProductScoreRepositoryPort productScoreRepositoryPort;
    @Mock private SalesRecordRepository salesRecordRepository;
    @Mock private ProductReviewRepository productReviewRepository;
    @Mock private HeatCompositeDailyRepository heatCompositeDailyRepository;
    @Mock private RiskAlertRepository riskAlertRepository;

    private final Category category = Category.builder().id(1L).name("零食").sortOrder(0).build();
    private final WeightVersion weightVersion = WeightVersion.builder().id(10L).versionNo("v1").name("test").build();
    private final OffsetDateTime asOf = OffsetDateTime.of(2026, 8, 27, 7, 0, 0, 0, ZoneOffset.ofHours(8));

    private ProductScoringOrchestrator newOrchestrator() {
        return new ProductScoringOrchestrator(
                productRepository, weightVersionRepository, weightProfileRepositoryPort,
                gradeThresholdRepositoryPort, riskRuleRepositoryPort, climateNormalRepositoryPort,
                audienceMixRepositoryPort, festivalAffinityRepositoryPort, productScoreRepositoryPort,
                salesRecordRepository, productReviewRepository, heatCompositeDailyRepository,
                riskAlertRepository);
    }

    private Product product(long id, BigDecimal marginRate) {
        return Product.builder()
                .id(id)
                .name("product-" + id)
                .category(category)
                .trackType(TrackType.A)
                .status(ProductStatus.EVALUATING)
                .season(Season.ALL)
                .marginRate(marginRate)
                .build();
    }

    /** §5.7：六項加分因子缺 4 項以上時不產生分數，呼叫端必須過濾、不得寫入。 */
    @Test
    void skipsProductWithInsufficientData() {
        Product product = product(1L, null); // 無成本/售價 → 六項因子全無資料
        when(productRepository.findScorable(TrackType.A)).thenReturn(List.of(product));
        when(weightVersionRepository.findByIsCurrentTrue()).thenReturn(Optional.of(weightVersion));
        when(weightProfileRepositoryPort.findWeights(10L, SceneType.REPLENISHMENT)).thenReturn(evenWeights());
        when(gradeThresholdRepositoryPort.findThresholds(10L, SceneType.REPLENISHMENT))
                .thenReturn(new GradeThresholdSet(SceneType.REPLENISHMENT, BigDecimal.valueOf(80), BigDecimal.valueOf(60)));
        when(productReviewRepository.countByProductId(1L)).thenReturn(5L); // < 20，樣本不足不查門檻

        ProductScoringOrchestrator.BatchSummary summary =
                newOrchestrator().runFullBatch(p -> SceneType.REPLENISHMENT, asOf);

        assertThat(summary.totalCandidates()).isEqualTo(1);
        assertThat(summary.scored()).isEqualTo(0);
        assertThat(summary.skippedInsufficientData()).isEqualTo(1);
        verify(productScoreRepositoryPort, never()).save(
                anyLong(), any(), any(), anyBoolean(), anyLong(), any(), anyInt(), any());
    }

    /**
     * §5.10 兩段式順序：product A／B 的 MARGIN 原始值不同（0.10／0.30），
     * 同品類母體必須是兩者合併——若批次內逐品項各自正規化（違反兩段式順序），
     * 兩者都會拿到「只有自己」的母體，percentile 恆為 100，測試會失敗。
     */
    @Test
    void normalizesAcrossWholeBatchNotPerProduct() {
        Product productA = product(1L, BigDecimal.valueOf(0.10));
        Product productB = product(2L, BigDecimal.valueOf(0.30));
        when(productRepository.findScorable(TrackType.A)).thenReturn(List.of(productA, productB));
        when(weightVersionRepository.findByIsCurrentTrue()).thenReturn(Optional.of(weightVersion));
        when(weightProfileRepositoryPort.findWeights(10L, SceneType.REPLENISHMENT)).thenReturn(evenWeights());
        when(gradeThresholdRepositoryPort.findThresholds(10L, SceneType.REPLENISHMENT))
                .thenReturn(new GradeThresholdSet(SceneType.REPLENISHMENT, BigDecimal.valueOf(80), BigDecimal.valueOf(60)));

        // CVR 與 PRICE_FIT 兩產品給相同資料，湊到「缺 3 項 < 4」門檻之上（FESTIVAL/CLIMATE/TREND 無資料）
        when(salesRecordRepository.findOwnConversionRate(anyLong())).thenReturn(0.2);
        when(audienceMixRepositoryPort.findMixForCategory(1L)).thenReturn(
                List.of(new AudienceSegmentShare("A1", BigDecimal.valueOf(100), BigDecimal.valueOf(200), BigDecimal.ONE)));
        productA.setSuggestedPrice(BigDecimal.valueOf(150));
        productB.setSuggestedPrice(BigDecimal.valueOf(150));
        when(festivalAffinityRepositoryPort.findAffinities(anyLong(), anyInt())).thenReturn(List.of());
        when(climateNormalRepositoryPort.findCategoryIdealTempRange(1L)).thenReturn(Optional.empty());
        when(productReviewRepository.countByProductId(anyLong())).thenReturn(5L);

        newOrchestrator().runFullBatch(p -> SceneType.REPLENISHMENT, asOf);

        ArgumentCaptor<ScoringResult> resultCaptor = ArgumentCaptor.forClass(ScoringResult.class);
        verify(productScoreRepositoryPort, org.mockito.Mockito.times(2)).save(
                anyLong(), eq("2026W35"), eq(SceneType.REPLENISHMENT), eq(true), eq(10L),
                resultCaptor.capture(), anyInt(), eq(asOf));

        List<ScoringResult> results = resultCaptor.getAllValues();
        BigDecimal marginPercentileA = marginNormalizedValue(results.get(0));
        BigDecimal marginPercentileB = marginNormalizedValue(results.get(1));

        assertThat(marginPercentileA).isEqualByComparingTo("25.00");
        assertThat(marginPercentileB).isEqualByComparingTo("75.00");
    }

    private BigDecimal marginNormalizedValue(ScoringResult result) {
        return result.factorContributions().stream()
                .filter(c -> c.factorCode() == FactorCode.MARGIN)
                .map(BonusFactorContribution::normalizedValue)
                .findFirst()
                .orElseThrow();
    }

    private Map<FactorCode, BigDecimal> evenWeights() {
        return Map.of(
                FactorCode.TREND, BigDecimal.valueOf(0.15),
                FactorCode.MARGIN, BigDecimal.valueOf(0.20),
                FactorCode.CVR, BigDecimal.valueOf(0.20),
                FactorCode.PRICE_FIT, BigDecimal.valueOf(0.20),
                FactorCode.FESTIVAL, BigDecimal.valueOf(0.15),
                FactorCode.CLIMATE, BigDecimal.valueOf(0.10));
    }
}
