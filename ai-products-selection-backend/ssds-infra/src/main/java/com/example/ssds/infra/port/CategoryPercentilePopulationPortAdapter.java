package com.example.ssds.infra.port;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.port.CategoryPercentilePopulationPort;
import com.example.ssds.infra.entity.Category;
import com.example.ssds.infra.repository.CategoryRepository;
import com.example.ssds.infra.repository.ScoreFactorRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 同品類百分位母體查詢（§5.3.1），由 {@code score_factor.raw_value} 取得。
 *
 * <p><b>對批次評分的隱含要求</b>：母體是「同一 period 內、同品類其他品項這個因子的
 * 原始值」。全量重評必須分兩段跑——第一段先把該批次全部品項的原始值寫入
 * {@code score_factor}（此時 normalized_value 可先留 null），第二段才呼叫本介面
 * 做正規化；不能一邊算一邊逐品項正規化，否則批次裡最先算的品項會拿到不完整的母體。
 * 這個排序由呼叫端（評分批次編排，屬 Phase 2）負責，本 adapter 只負責查詢。
 */
@Component
public class CategoryPercentilePopulationPortAdapter implements CategoryPercentilePopulationPort {

    private final ScoreFactorRepository scoreFactorRepository;
    private final CategoryRepository categoryRepository;

    public CategoryPercentilePopulationPortAdapter(
            ScoreFactorRepository scoreFactorRepository, CategoryRepository categoryRepository) {
        this.scoreFactorRepository = scoreFactorRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<BigDecimal> findOwnCategoryValues(FactorCode factorCode, long categoryId, String period) {
        return scoreFactorRepository.findRawValuesByCategory(factorCode, categoryId, period);
    }

    @Override
    public List<BigDecimal> findSiblingMergedValues(FactorCode factorCode, long categoryId, String period) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("找不到品類: categoryId=" + categoryId));
        Category parent = category.getParent();
        if (parent == null) {
            // 頂層品類沒有兄弟品類可合併；依 §5.3.1，own 樣本 3–9 卻無父品類是資料建置的
            // 邊界情況（兩層品類架構下的頂層本身就該有足夠樣本），回空清單讓呼叫端知道退路失敗。
            return List.of();
        }
        return scoreFactorRepository.findRawValuesByParentCategory(factorCode, parent.getId(), period);
    }

    @Override
    public List<BigDecimal> findAllCategoryValues(FactorCode factorCode, String period) {
        return scoreFactorRepository.findRawValuesAll(factorCode, period);
    }
}
