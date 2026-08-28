package com.example.ssds.api.risk;

import com.example.ssds.api.common.error.BusinessException;
import com.example.ssds.api.common.error.ErrorCode;
import com.example.ssds.api.common.util.ApiTime;
import com.example.ssds.api.dto.RiskAlertListItem;
import com.example.ssds.core.domain.AlertStatus;
import com.example.ssds.core.domain.Severity;
import com.example.ssds.infra.entity.AppUser;
import com.example.ssds.infra.entity.RiskAlert;
import com.example.ssds.infra.repository.AppUserRepository;
import com.example.ssds.infra.repository.RiskAlertRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-10 風險示警中心。門檻調整（§FR-10-3 觸發背景全量扣分重算）待評分批次排程接上後再做，見 docs/module-tasks.md。 */
@Service
public class RiskAlertService {

    private final RiskAlertRepository riskAlertRepository;
    private final AppUserRepository appUserRepository;

    public RiskAlertService(RiskAlertRepository riskAlertRepository, AppUserRepository appUserRepository) {
        this.riskAlertRepository = riskAlertRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public Page<RiskAlertListItem> list(
            AlertStatus status, Severity severity, String riskType, Long categoryId, Pageable pageable) {
        var spec = RiskAlertSpecifications.combine(
                RiskAlertSpecifications.statusEquals(status),
                RiskAlertSpecifications.severityEquals(severity),
                RiskAlertSpecifications.typeEquals(riskType),
                RiskAlertSpecifications.categoryIdEquals(categoryId));
        return riskAlertRepository.findAll(spec, pageable).map(this::toListItem);
    }

    /** §FR-10 操作「標記已處理」：OPEN → ACKNOWLEDGED。 */
    @Transactional
    public RiskAlertListItem acknowledge(Long id, Long actingUserId) {
        RiskAlert alert = require(id);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setHandledAt(Instant.now());
        alert.setHandledBy(actor(actingUserId));
        return toListItem(alert);
    }

    /** §FR-10 操作「忽略」：理由必填（DB 端 {@code ck_risk_ignore_reason} 亦有 CHECK）。 */
    @Transactional
    public RiskAlertListItem ignore(Long id, String reason, Long actingUserId) {
        RiskAlert alert = require(id);
        alert.setStatus(AlertStatus.IGNORED);
        alert.setIgnoreReason(reason);
        alert.setHandledAt(Instant.now());
        alert.setHandledBy(actor(actingUserId));
        return toListItem(alert);
    }

    private RiskAlert require(Long id) {
        return riskAlertRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AppUser actor(Long userId) {
        return userId == null ? null : appUserRepository.getReferenceById(userId);
    }

    private RiskAlertListItem toListItem(RiskAlert a) {
        return new RiskAlertListItem(
                a.getId(),
                a.getProduct().getId(), a.getProduct().getName(),
                a.getProduct().getCategory().getId(), a.getProduct().getCategory().getName(),
                a.getRiskType(), a.getSeverity(), a.getTriggerValue(), a.getStatus(), a.getIgnoreReason(),
                ApiTime.from(a.getDetectedAt()), ApiTime.from(a.getHandledAt()),
                a.getHandledBy() == null ? null : a.getHandledBy().getDisplayName());
    }
}
