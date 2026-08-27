package com.example.ssds.infra.repository;

import com.example.ssds.infra.entity.SalesRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 銷售紀錄（規格書 §7.2 sales_record）。走 idx_sales_date_cat。 */
@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {

    List<SalesRecord> findByProductIdAndOrderDateBetween(
            Long productId, LocalDate from, LocalDate to);

    List<SalesRecord> findByCategoryIdAndOrderDateBetween(
            Long categoryId, LocalDate from, LocalDate to);

    List<SalesRecord> findByProductId(Long productId);

    /**
     * 品項自身的歷史轉換率（§5.2.3 第一順位）。規格未定義時間窗，一律採全部歷史開團紀錄
     * （不限日期範圍）。曝光數合計為 0 或 null（即無任何 impression 有效的紀錄）時回傳
     * null，由呼叫端走 §5.2.3 其餘退路，不要當成轉換率 0（§5.7）。
     */
    @Query("""
            select case when coalesce(sum(s.impression), 0) = 0 then null
                        else cast(sum(s.qty) as double) / cast(sum(s.impression) as double) end
            from SalesRecord s
            where s.product.id = :productId and s.impression is not null and s.impression > 0
            """)
    Double findOwnConversionRate(@Param("productId") Long productId);

    /**
     * 同品類各筆銷售紀錄的轉換率（§5.2.3 第二順位「同品類中位數」用），只取
     * impression 有效的紀錄；呼叫端負責計算中位數與判斷筆數是否達 10 筆門檻。
     */
    @Query("""
            select cast(s.qty as double) / cast(s.impression as double)
            from SalesRecord s
            where s.category.id = :categoryId and s.impression is not null and s.impression > 0
            """)
    List<Double> findConversionRatiosByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 同品類、同一日期範圍內各品項的 qty 加總（§5.2.3 第三順位「qty / 品類同期平均 qty」
     * 用，適用於曝光數全缺的品項）。
     */
    @Query("""
            select s.product.id as productId, sum(s.qty) as totalQty
            from SalesRecord s
            where s.category.id = :categoryId and s.product is not null
                  and s.orderDate between :from and :to
            group by s.product.id
            """)
    List<CategoryProductQty> sumQtyByProductInCategoryAndDateRange(
            @Param("categoryId") Long categoryId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    interface CategoryProductQty {
        Long getProductId();

        Long getTotalQty();
    }

    List<SalesRecord> findByImportBatchId(Long batchId);

    /** 比對不到品項的列，供後續人工對應。 */
    List<SalesRecord> findByProductIsNull();
}
