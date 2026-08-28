package com.example.ssds.api.product;

import com.example.ssds.api.common.error.BusinessException;
import com.example.ssds.api.common.error.ErrorCode;
import com.example.ssds.api.common.util.ApiTime;
import com.example.ssds.api.dto.ProductDetail;
import com.example.ssds.api.dto.ProductListItem;
import com.example.ssds.api.dto.ProductRequest;
import com.example.ssds.api.dto.ProductSaveResult;
import com.example.ssds.core.domain.ProductStatus;
import com.example.ssds.core.domain.TrackType;
import com.example.ssds.infra.entity.AppUser;
import com.example.ssds.infra.entity.Category;
import com.example.ssds.infra.entity.Product;
import com.example.ssds.infra.entity.ProductScore;
import com.example.ssds.infra.entity.Supplier;
import com.example.ssds.infra.entity.TrendKeyword;
import com.example.ssds.infra.repository.AppUserRepository;
import com.example.ssds.infra.repository.CategoryRepository;
import com.example.ssds.infra.repository.ProductRepository;
import com.example.ssds.infra.repository.ProductScoreRepository;
import com.example.ssds.infra.repository.SupplierRepository;
import com.example.ssds.infra.repository.TrendKeywordRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-03 品項管理。§7.4 狀態機的直接轉換（不經決策）只有 DRAFT→EVALUATING、
 * ADOPTED→LISTED，其餘轉換（WATCHING/ADOPTED/REJECTED）綁在「建立決策」（FR-11，
 * 尚未實作），本服務對這些一律回 409 INVALID_STATE_TRANSITION。
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final TrendKeywordRepository trendKeywordRepository;
    private final ProductScoreRepository productScoreRepository;
    private final AppUserRepository appUserRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            TrendKeywordRepository trendKeywordRepository,
            ProductScoreRepository productScoreRepository,
            AppUserRepository appUserRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.trendKeywordRepository = trendKeywordRepository;
        this.productScoreRepository = productScoreRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductListItem> list(
            String keyword, Long categoryId, Long supplierId, TrackType trackType,
            com.example.ssds.core.domain.SourcingStatus sourcingStatus, ProductStatus status,
            Pageable pageable) {
        var spec = ProductSpecifications.combine(
                ProductSpecifications.nameContains(keyword),
                ProductSpecifications.categoryIdEquals(categoryId),
                ProductSpecifications.supplierIdEquals(supplierId),
                ProductSpecifications.trackTypeEquals(trackType),
                ProductSpecifications.sourcingStatusEquals(sourcingStatus),
                ProductSpecifications.statusEquals(status));
        Page<Product> page = productRepository.findAll(spec, pageable);

        List<Long> ids = page.getContent().stream().map(Product::getId).toList();
        Map<Long, ProductScore> latestScoreByProductId = ids.isEmpty()
                ? Map.of()
                : productScoreRepository.findActivePrimaryByProductIds(ids).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                s -> s.getProduct().getId(), Function.identity(), (a, b) -> a));

        return page.map(p -> toListItem(p, latestScoreByProductId.get(p.getId())));
    }

    @Transactional(readOnly = true)
    public ProductDetail get(Long id) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toDetail(product);
    }

    @Transactional
    public ProductSaveResult create(ProductRequest request, Long actingUserId) {
        Category category = requireCategory(request.categoryId());
        Supplier supplier = requireSupplier(request.supplierId());
        Set<TrendKeyword> keywords = requireKeywords(request.keywordIds());
        validateTrackRequirements(request);
        validatePricing(request.cost(), request.suggestedPrice());

        boolean duplicateName = productRepository.existsByCategoryIdAndNameIgnoreCase(
                request.categoryId(), request.name());

        Product product = Product.builder()
                .name(request.name())
                .category(category)
                .supplier(supplier)
                .cost(request.cost())
                .suggestedPrice(request.suggestedPrice())
                .moq(request.moq())
                .season(request.season() == null ? com.example.ssds.core.domain.Season.ALL : request.season())
                .status(ProductStatus.DRAFT)
                .trackType(request.trackType())
                .sourcingStatus(request.trackType() == TrackType.B
                        ? (request.sourcingStatus() == null
                                ? com.example.ssds.core.domain.SourcingStatus.PENDING
                                : request.sourcingStatus())
                        : null)
                .logisticsCondition(request.logisticsCondition())
                .shelfLifeDays(request.shelfLifeDays())
                .idealTempMin(request.idealTempMin())
                .idealTempMax(request.idealTempMax())
                .keywords(new LinkedHashSet<>(keywords))
                .createdBy(actingUserId == null ? null : appUserRepository.getReferenceById(actingUserId))
                .build();

        product = productRepository.save(product);
        return new ProductSaveResult(toDetail(product), duplicateName);
    }

    @Transactional
    public ProductSaveResult update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Category category = requireCategory(request.categoryId());
        Supplier supplier = requireSupplier(request.supplierId());
        Set<TrendKeyword> keywords = requireKeywords(request.keywordIds());
        validateTrackRequirements(request);
        validatePricing(request.cost(), request.suggestedPrice());

        boolean duplicateName = productRepository.existsDuplicateName(
                request.categoryId(), request.name(), id);

        product.setName(request.name());
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setCost(request.cost());
        product.setSuggestedPrice(request.suggestedPrice());
        product.setMoq(request.moq());
        if (request.season() != null) {
            product.setSeason(request.season());
        }
        product.setTrackType(request.trackType());
        product.setSourcingStatus(request.trackType() == TrackType.B ? request.sourcingStatus() : null);
        product.setLogisticsCondition(request.logisticsCondition());
        product.setShelfLifeDays(request.shelfLifeDays());
        product.setIdealTempMin(request.idealTempMin());
        product.setIdealTempMax(request.idealTempMax());
        product.setKeywords(new LinkedHashSet<>(keywords));

        return new ProductSaveResult(toDetail(product), duplicateName);
    }

    @Transactional
    public ProductDetail changeStatus(Long id, ProductStatus target) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ProductStatus current = product.getStatus();

        boolean allowed = (current == ProductStatus.DRAFT && target == ProductStatus.EVALUATING)
                || (current == ProductStatus.ADOPTED && target == ProductStatus.LISTED);
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "品項狀態不允許由 " + current + " 直接變更為 " + target);
        }

        product.setStatus(target);
        if (target == ProductStatus.LISTED) {
            product.setListedAt(java.time.LocalDate.now());
        }
        return toDetail(product);
    }

    @Transactional
    public void softDelete(Long id, Long actingUserId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        AppUser actor = actingUserId == null ? null : appUserRepository.getReferenceById(actingUserId);
        product.softDelete(actor);
    }

    private void validateTrackRequirements(ProductRequest request) {
        if (request.trackType() == TrackType.A) {
            if (request.cost() == null || request.suggestedPrice() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "A 軌品項必須填寫成本與建議售價");
            }
        } else if (request.keywordIds() == null || request.keywordIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "B 軌品項必須至少關聯一個關鍵字");
        }
    }

    /** FR-03-2 例外條件：A 軌成本 ≥ 售價 → 阻擋儲存。 */
    private void validatePricing(java.math.BigDecimal cost, java.math.BigDecimal suggestedPrice) {
        if (cost != null && suggestedPrice != null && suggestedPrice.compareTo(cost) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "建議售價必須大於成本");
        }
    }

    private Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "指定的類別不存在"));
    }

    private Supplier requireSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "指定的供應商不存在"));
    }

    private Set<TrendKeyword> requireKeywords(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return Set.of();
        }
        List<TrendKeyword> found = trendKeywordRepository.findAllById(keywordIds);
        if (found.size() != new LinkedHashSet<>(keywordIds).size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "包含不存在的關鍵字");
        }
        return new LinkedHashSet<>(found);
    }

    private ProductListItem toListItem(Product p, ProductScore score) {
        return new ProductListItem(
                p.getId(), p.getName(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getTrackType(),
                p.getSupplier() == null ? null : p.getSupplier().getId(),
                p.getSupplier() == null ? null : p.getSupplier().getName(),
                p.getCost(), p.getSuggestedPrice(), p.getMarginRate(),
                score == null ? null : score.getFinalScore(),
                score == null ? null : score.getGrade(),
                p.getStatus(), p.getSourcingStatus(), p.getLastScoringStatus());
    }

    private ProductDetail toDetail(Product p) {
        return new ProductDetail(
                p.getId(), p.getName(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getSupplier() == null ? null : p.getSupplier().getId(),
                p.getSupplier() == null ? null : p.getSupplier().getName(),
                p.getCost(), p.getSuggestedPrice(), p.getMarginRate(),
                p.getMoq(), p.getSeason(), p.getStatus(), p.getRejectReason(), p.getListedAt(),
                p.getTrackType(), p.getSourcingStatus(), p.getLogisticsCondition(), p.getShelfLifeDays(),
                p.getIdealTempMin(), p.getIdealTempMax(),
                p.getLastScoringStatus(), ApiTime.from(p.getLastScoringAttemptedAt()),
                p.getKeywords().stream().map(TrendKeyword::getId).toList(),
                p.getKeywords().stream().map(TrendKeyword::getKeyword).toList(),
                ApiTime.from(p.getCreatedAt()), ApiTime.from(p.getUpdatedAt()));
    }
}
