package com.example.ssds.infra.port;

import com.example.ssds.core.domain.SceneType;
import com.example.ssds.core.port.ProductScoreRepositoryPort;
import com.example.ssds.core.scoring.BonusFactorContribution;
import com.example.ssds.core.scoring.PenaltyContribution;
import com.example.ssds.core.scoring.ScoringResult;
import com.example.ssds.infra.entity.Product;
import com.example.ssds.infra.entity.ProductScore;
import com.example.ssds.infra.entity.ScoreFactor;
import com.example.ssds.infra.entity.WeightVersion;
import com.example.ssds.infra.repository.ProductRepository;
import com.example.ssds.infra.repository.ProductScoreRepository;
import com.example.ssds.infra.repository.WeightVersionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 評分結果寫入（規格書 §5.10）。同 (product, period, sceneType) 的舊現行列先改為
 * 非現行，才寫入新紀錄——舊紀錄保留不刪除，只有 {@code is_active} 改變。
 */
@Component
public class ProductScoreRepositoryPortAdapter implements ProductScoreRepositoryPort {

    private final ProductScoreRepository productScoreRepository;
    private final ProductRepository productRepository;
    private final WeightVersionRepository weightVersionRepository;

    public ProductScoreRepositoryPortAdapter(
            ProductScoreRepository productScoreRepository,
            ProductRepository productRepository,
            WeightVersionRepository weightVersionRepository) {
        this.productScoreRepository = productScoreRepository;
        this.productRepository = productRepository;
        this.weightVersionRepository = weightVersionRepository;
    }

    @Override
    @Transactional
    public void save(
            long productId, String period, SceneType sceneType, boolean isPrimary,
            long weightVersionId, ScoringResult result, int confidence, OffsetDateTime calculatedAt) {

        if (!result.sufficientData()) {
            throw new IllegalArgumentException(
                    "資料不足（§5.7）不產生 product_score 列，呼叫端須在呼叫前過濾，不可寫入 grade 為 null 的紀錄");
        }

        List<ProductScore> currentlyActive = productScoreRepository
                .findByProductIdAndPeriodAndSceneTypeAndActiveTrue(productId, period, sceneType);
        currentlyActive.forEach(score -> score.setActive(false));
        productScoreRepository.saveAll(currentlyActive);

        Product product = productRepository.getReferenceById(productId);
        WeightVersion weightVersion = weightVersionRepository.getReferenceById(weightVersionId);

        ProductScore score = ProductScore.builder()
                .product(product)
                .weightVersion(weightVersion)
                .period(period)
                .sceneType(sceneType)
                .primary(isPrimary)
                .active(true)
                .bonusSubtotal(result.bonusSubtotal())
                .penaltySubtotal(result.penaltySubtotal())
                .finalScore(result.finalScore())
                .grade(result.grade())
                .confidence(confidence)
                .calculatedAt(calculatedAt.toInstant())
                .build();

        for (BonusFactorContribution bonus : result.factorContributions()) {
            score.getFactors().add(ScoreFactor.builder()
                    .score(score)
                    .factorCode(bonus.factorCode())
                    .penalty(false)
                    .rawValue(bonus.rawValue())
                    .normalizedValue(bonus.normalizedValue())
                    .weight(bonus.dataAvailable() ? bonus.weight() : null)
                    .dataAvailable(bonus.dataAvailable())
                    .imputed(bonus.imputed())
                    .note(bonus.note())
                    .build());
        }
        for (PenaltyContribution penalty : result.penaltyContributions()) {
            score.getFactors().add(ScoreFactor.builder()
                    .score(score)
                    .factorCode(penalty.factorCode())
                    .penalty(true)
                    .penaltyValue(penalty.penaltyValue())
                    .dataAvailable(true)
                    .imputed(false)
                    .note(penalty.note())
                    .build());
        }

        productScoreRepository.save(score);
    }
}
