package com.example.ssds.infra.port;

import com.example.ssds.core.port.RiskRuleConfig;
import com.example.ssds.core.port.RiskRuleRepositoryPort;
import com.example.ssds.infra.entity.RiskRule;
import com.example.ssds.infra.repository.RiskRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 有品類覆寫用品類，否則退回全域預設（§7.2 risk_rule）。 */
@Component
public class RiskRuleRepositoryPortAdapter implements RiskRuleRepositoryPort {

    // 自建而非注入容器的 ObjectMapper：ssds-infra 不依賴 web/json starter，
    // 只解析固定形狀的 threshold_json，同 RestAuthenticationEntryPoint 的作法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RiskRuleRepository riskRuleRepository;

    public RiskRuleRepositoryPortAdapter(RiskRuleRepository riskRuleRepository) {
        this.riskRuleRepository = riskRuleRepository;
    }

    @Override
    public RiskRuleConfig findConfig(String ruleCode, Long categoryId) {
        Optional<RiskRule> categoryOverride = categoryId == null
                ? Optional.empty()
                : riskRuleRepository.findByRuleCodeAndCategoryId(ruleCode, categoryId);

        RiskRule rule = categoryOverride
                .or(() -> riskRuleRepository.findByRuleCodeAndCategoryIsNull(ruleCode))
                .orElseThrow(() -> new IllegalStateException("找不到扣分規則: " + ruleCode));

        return new RiskRuleConfig(
                rule.getRuleCode(), rule.getCategory() == null ? null : rule.getCategory().getId(),
                parseThresholds(rule.getThresholdJson()), rule.getMaxPenalty());
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> parseThresholds(String thresholdJson) {
        try {
            return objectMapper.readValue(thresholdJson, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("risk_rule.threshold_json 格式錯誤: " + thresholdJson, e);
        }
    }
}
