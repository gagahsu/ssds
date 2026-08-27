package com.example.ssds.infra.repository;

import com.example.ssds.core.domain.Sentiment;
import com.example.ssds.infra.entity.ProductReview;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 評論查詢（規格書 §7.2 product_review）。 */
@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    @EntityGraph(attributePaths = {"analysis"})
    Page<ProductReview> findByProductId(Long productId, Pageable pageable);

    /** 匯入前的重複檢查；正常路徑靠唯一鍵擋，這支供預覽階段提示使用者。 */
    boolean existsByProductIdAndContentHash(Long productId, String contentHash);

    /** 尚未分析的評論，供 ReviewRiskAgent 批次處理。 */
    @Query("select r from ProductReview r where r.analysis is null and r.product.id = :productId")
    List<ProductReview> findUnanalyzedByProduct(@Param("productId") Long productId);

    long countByProductId(Long productId);

    /** REVIEW_RISK 扣分（§5.2.2）的負評則數：僅計已分析且情感為負面者。 */
    long countByProductIdAndAnalysisSentiment(Long productId, Sentiment sentiment);

    /**
     * 負評的關鍵詞，供 {@code risk_topic_share}（品質／食安／物流破損佔比，§5.2.2）判定。
     * Agent 2 ReviewRiskAgent（Track 3，尚未實作）本應直接輸出風險主題分類；在那之前
     * 由呼叫端對 {@code key_phrase} 做關鍵字比對，是暫時性的替代方案。
     */
    @Query("select r.analysis.keyPhrase from ProductReview r "
            + "where r.product.id = :productId and r.analysis.sentiment = :sentiment")
    List<String> findKeyPhrasesByProductIdAndAnalysisSentiment(
            @Param("productId") Long productId, @Param("sentiment") Sentiment sentiment);
}
