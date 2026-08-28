package com.example.ssds.api.product;

import com.example.ssds.core.domain.ProductStatus;
import com.example.ssds.core.domain.SourcingStatus;
import com.example.ssds.core.domain.TrackType;
import com.example.ssds.infra.entity.Product;
import org.springframework.data.jpa.domain.Specification;

/**
 * §8.2 {@code GET /products} 查詢參數轉 JPA Specification。
 *
 * <p>{@code grade}／{@code minScore}／{@code maxScore}／{@code hasRisk} 這四個 v3.0
 * 查詢參數需要 join {@code product_score}／{@code risk_alert} 才能實作，
 * 屬於 §FR-04 排行/評分那條線的資料，目前 controller 尚未接，先不受理
 * （傳了會被忽略，不是回錯誤——同一份清單頁在評分批次接上之前仍要能用）。
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Product> categoryIdEquals(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> supplierIdEquals(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("supplier").get("id"), supplierId);
    }

    public static Specification<Product> trackTypeEquals(TrackType trackType) {
        if (trackType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("trackType"), trackType);
    }

    public static Specification<Product> sourcingStatusEquals(SourcingStatus sourcingStatus) {
        if (sourcingStatus == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("sourcingStatus"), sourcingStatus);
    }

    public static Specification<Product> statusEquals(ProductStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    @SafeVarargs
    public static Specification<Product> combine(Specification<Product>... specs) {
        return Specification.allOf(java.util.Arrays.stream(specs)
                .filter(java.util.Objects::nonNull)
                .toList());
    }
}
