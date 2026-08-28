package com.example.ssds.api.controller;

import com.example.ssds.api.common.response.ApiResponse;
import com.example.ssds.api.common.response.PageResponse;
import com.example.ssds.api.dto.ProductDetail;
import com.example.ssds.api.dto.ProductListItem;
import com.example.ssds.api.dto.ProductRequest;
import com.example.ssds.api.dto.ProductSaveResult;
import com.example.ssds.api.dto.ProductStatusChangeRequest;
import com.example.ssds.api.product.ProductService;
import com.example.ssds.core.domain.ProductStatus;
import com.example.ssds.core.domain.SourcingStatus;
import com.example.ssds.core.domain.TrackType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FR-03 品項管理、§8.2 /products 端點。權限依 §2.1 權限矩陣第 2／4／5 列。 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN', 'VIEWER')")
    public ApiResponse<PageResponse<ProductListItem>> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) TrackType trackType,
            @RequestParam(required = false) SourcingStatus sourcingStatus,
            @RequestParam(required = false) ProductStatus status,
            Pageable pageable) {
        var page = productService.list(keyword, categoryId, supplierId, trackType, sourcingStatus, status, pageable);
        return ApiResponse.success(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN', 'VIEWER')")
    public ApiResponse<ProductDetail> get(@PathVariable Long id) {
        return ApiResponse.success(productService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN')")
    public ApiResponse<ProductSaveResult> createProduct(
            @Valid @RequestBody ProductRequest request, @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(productService.create(request, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN')")
    public ApiResponse<ProductSaveResult> updateProduct(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN')")
    public ApiResponse<ProductDetail> changeStatus(
            @PathVariable Long id, @Valid @RequestBody ProductStatusChangeRequest request) {
        return ApiResponse.success(productService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER_LEAD', 'SYS_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        productService.softDelete(id, userId);
        return ApiResponse.success(null);
    }
}
