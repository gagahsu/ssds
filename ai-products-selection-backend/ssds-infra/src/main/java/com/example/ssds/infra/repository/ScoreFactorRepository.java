package com.example.ssds.infra.repository;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.infra.entity.ScoreFactor;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 因子明細（規格書 §7.2 score_factor）。
 *
 * <p>畫面上每根長條對應一列，因此查詢一律以 score_id 為條件整批取出，
 * 不要逐個因子查。
 */
@Repository
public interface ScoreFactorRepository extends JpaRepository<ScoreFactor, Long> {

    List<ScoreFactor> findByScoreId(Long scoreId);

    /** 加分區塊與扣分區塊在 UI 上是分開的兩塊（FR-05 區塊 C／D）。 */
    List<ScoreFactor> findByScoreIdAndPenalty(Long scoreId, boolean penalty);

    List<ScoreFactor> findByScoreIdAndFactorCode(Long scoreId, FactorCode factorCode);

    /** §5.7：缺資料的因子要在 UI 標灰底。 */
    List<ScoreFactor> findByScoreIdAndDataAvailableFalse(Long scoreId);

    /**
     * §5.3.1 同品類百分位的母體：同一 period、該品類下所有品項這個因子的原始值
     * （只取有資料的，缺資料的因子不該污染母體）。
     *
     * <p>不限 {@code is_active}：全量重評時，同批次內其他品項當下也還在寫入同一
     * period 的新列，此時它們尚未成為「現行」分數，但仍是這一期母體的一部分。
     */
    @Query("""
            select f.rawValue from ScoreFactor f
            where f.factorCode = :factorCode
              and f.dataAvailable = true
              and f.score.period = :period
              and f.score.product.category.id = :categoryId
            """)
    List<BigDecimal> findRawValuesByCategory(
            @Param("factorCode") FactorCode factorCode,
            @Param("categoryId") Long categoryId,
            @Param("period") String period);

    /** 同一父品類下所有兄弟品類（含自己）合併母體，§5.3.1 樣本 3–9 的退路。 */
    @Query("""
            select f.rawValue from ScoreFactor f
            where f.factorCode = :factorCode
              and f.dataAvailable = true
              and f.score.period = :period
              and f.score.product.category.parent.id = :parentCategoryId
            """)
    List<BigDecimal> findRawValuesByParentCategory(
            @Param("factorCode") FactorCode factorCode,
            @Param("parentCategoryId") Long parentCategoryId,
            @Param("period") String period);

    /** 全品類母體，§5.3.1 樣本 < 3 的退路。 */
    @Query("""
            select f.rawValue from ScoreFactor f
            where f.factorCode = :factorCode
              and f.dataAvailable = true
              and f.score.period = :period
            """)
    List<BigDecimal> findRawValuesAll(
            @Param("factorCode") FactorCode factorCode, @Param("period") String period);
}
