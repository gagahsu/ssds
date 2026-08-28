package com.example.ssds.api.controller;

import com.example.ssds.api.common.response.ApiResponse;
import com.example.ssds.api.dto.WeightVersionDetail;
import com.example.ssds.api.dto.WeightVersionSummary;
import com.example.ssds.api.dto.WeightVersionUpsertRequest;
import com.example.ssds.api.weight.WeightVersionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-08 情境權重組設定、§8.2 /weight-versions 端點。權限依 §2.1 第 2／15 列。
 * {@code POST /{id}/approve} 未實作，見 {@link WeightVersionService} 類別註解。
 */
@RestController
@RequestMapping("/weight-versions")
public class WeightVersionController {

    private final WeightVersionService weightVersionService;

    public WeightVersionController(WeightVersionService weightVersionService) {
        this.weightVersionService = weightVersionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN', 'VIEWER')")
    public ApiResponse<List<WeightVersionSummary>> listWeightVersions() {
        return ApiResponse.success(weightVersionService.list());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN', 'VIEWER')")
    public ApiResponse<WeightVersionDetail> active() {
        return ApiResponse.success(weightVersionService.active());
    }

    @GetMapping("/{id}/profiles")
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN', 'VIEWER')")
    public ApiResponse<WeightVersionDetail> profiles(@PathVariable Long id) {
        return ApiResponse.success(weightVersionService.profiles(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER_LEAD')")
    public ApiResponse<WeightVersionDetail> createWeightVersion(
            @Valid @RequestBody WeightVersionUpsertRequest request, @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(weightVersionService.create(request, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUYER_LEAD')")
    public ApiResponse<WeightVersionDetail> updateWeightVersion(
            @PathVariable Long id, @Valid @RequestBody WeightVersionUpsertRequest request) {
        return ApiResponse.success(weightVersionService.update(id, request));
    }
}
