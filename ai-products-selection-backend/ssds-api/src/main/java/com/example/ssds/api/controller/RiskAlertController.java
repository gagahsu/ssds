package com.example.ssds.api.controller;

import com.example.ssds.api.common.response.ApiResponse;
import com.example.ssds.api.common.response.PageResponse;
import com.example.ssds.api.dto.RiskAlertListItem;
import com.example.ssds.api.dto.RiskIgnoreRequest;
import com.example.ssds.api.risk.RiskAlertService;
import com.example.ssds.core.domain.AlertStatus;
import com.example.ssds.core.domain.Severity;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FR-10 風險示警中心、§8.2 /risks 端點。權限依 §2.1 權限矩陣第 2／14 列。 */
@RestController
@RequestMapping("/risks")
public class RiskAlertController {

    private final RiskAlertService riskAlertService;

    public RiskAlertController(RiskAlertService riskAlertService) {
        this.riskAlertService = riskAlertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'DATA_ADMIN', 'SYS_ADMIN', 'VIEWER')")
    public ApiResponse<PageResponse<RiskAlertListItem>> listRisks(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        var page = riskAlertService.list(status, severity, type, categoryId, pageable);
        return ApiResponse.success(PageResponse.from(page));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'SYS_ADMIN')")
    public ApiResponse<RiskAlertListItem> acknowledge(
            @PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(riskAlertService.acknowledge(id, userId));
    }

    @PatchMapping("/{id}/ignore")
    @PreAuthorize("hasAnyRole('BUYER', 'BUYER_LEAD', 'SYS_ADMIN')")
    public ApiResponse<RiskAlertListItem> ignore(
            @PathVariable Long id, @Valid @RequestBody RiskIgnoreRequest request,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(riskAlertService.ignore(id, request.reason(), userId));
    }
}
