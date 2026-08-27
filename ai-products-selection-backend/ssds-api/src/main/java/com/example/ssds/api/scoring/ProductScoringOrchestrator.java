package com.example.ssds.api.scoring;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.LogisticsCondition;
import com.example.ssds.core.domain.SceneType;
import com.example.ssds.core.domain.Sentiment;
import com.example.ssds.core.domain.TrackType;
import com.example.ssds.core.port.AudienceMixRepositoryPort;
import com.example.ssds.core.port.ClimateNormalRepositoryPort;
import com.example.ssds.core.port.FestivalAffinityRepositoryPort;
import com.example.ssds.core.port.GradeThresholdRepositoryPort;
import com.example.ssds.core.port.ProductScoreRepositoryPort;
import com.example.ssds.core.port.RiskRuleRepositoryPort;
import com.example.ssds.core.port.WeightProfileRepositoryPort;
import com.example.ssds.core.scoring.AudienceSegmentShare;
import com.example.ssds.core.scoring.BonusFactorInput;
import com.example.ssds.core.scoring.ClimateFitCalculator;
import com.example.ssds.core.scoring.ConfidenceCalculator;
import com.example.ssds.core.scoring.ConfidencePenaltyReason;
import com.example.ssds.core.scoring.FestivalAffinityInput;
import com.example.ssds.core.scoring.FestivalFactorResult;
import com.example.ssds.core.scoring.FestivalWindowCalculator;
import com.example.ssds.core.scoring.GradeThresholdSet;
import com.example.ssds.core.scoring.InventoryRiskCalculator;
import com.example.ssds.core.scoring.LogisticsRiskCalculator;
import com.example.ssds.core.scoring.NormalizationResult;
import com.example.ssds.core.scoring.PenaltyContribution;
import com.example.ssds.core.scoring.PercentileNormalizer;
import com.example.ssds.core.scoring.PriceFitCalculator;
import com.example.ssds.core.scoring.ReviewRiskCalculator;
import com.example.ssds.core.scoring.ScoringEngine;
import com.example.ssds.core.scoring.ScoringResult;
import com.example.ssds.core.scoring.TrendSlopeCalculator;
import com.example.ssds.core.scoring.TrendSlopeResult;
import com.example.ssds.infra.entity.Category;
import com.example.ssds.infra.entity.Product;
import com.example.ssds.infra.entity.SalesRecord;
import com.example.ssds.infra.entity.TrendKeyword;
import com.example.ssds.infra.entity.WeightVersion;
import com.example.ssds.infra.repository.HeatCompositeDailyRepository;
import com.example.ssds.infra.repository.ProductRepository;
import com.example.ssds.infra.repository.ProductReviewRepository;
import com.example.ssds.infra.repository.SalesRecordRepository;
import com.example.ssds.infra.repository.WeightVersionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 評分批次編排（規格書 §5.10），把 {@code ssds-core} 的計算器與 {@code ssds-infra} 的
 * 資料存取串成一次真正的評分。目前只有這裡把兩者接起來——之前兩個 track 各自完工後
 * 從未被呼叫過。
 *
 * <p><b>情境判定不在本類別的職責內</b>：Agent 1 SceneClassifierAgent（Track 3，
 * {@code ssds-ai}）尚未實作，情境由呼叫端透過 {@link SceneResolver} 提供
 * （排程/API 觸發點接上 Track 3 後，換一個真的呼叫 LLM 的實作即可，本類別不必改）。
 *
 * <p><b>§5.10 兩段式批次順序的落地方式</b>：正規化母體理論上該讀
 * {@code score_factor.raw_value}（見 {@code CategoryPercentilePopulationPort} 的實作），
 * 但目前 {@code product_score.grade} 是 DB NOT NULL（module-tasks.md 已知問題 2 待補的
 * schema 變更），批次內任何一個品項在算完六項因子前都無法先寫入一筆「未完成」的
 * product_score／score_factor 列。因此兩段式的「寫入」實際上只發生一次（每品項一次
 * 終局寫入，經 {@link ProductScoreRepositoryPort#save}），但「先算完全部品項的原始值、
 * 再統一正規化」的順序本身完全遵守：第一段在記憶體中蒐集整批品項六項加分因子的
 * raw_value（見 {@link #computeRawFactors}），母體只在整批算完後才組出來
 * （見 {@link #buildPopulation}），第二段才對每個品項做同品類百分位正規化並寫入
 * 最終結果。等前述 schema 缺口補上後，可以改成真的分兩次交易寫 DB，
 * 屆時同品類母體也能直接沿用 {@code CategoryPercentilePopulationPort}。
 */
@Service
public class ProductScoringOrchestrator {

    /**
     * CLIMATE 因子的地區碼；品項/品類目前都沒有地區欄位，先固定為附錄 B
     * {@code CLIMATE_REGION_DEFAULT} 的預設值（§FR-17-2 待補區域化）。
     */
    private static final String DEFAULT_REGION_CODE = "TW_TPE";
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    /** CVR 第二／四順位「同品類筆數」門檻（§5.2.3：≥10 用中位數，< 10 標無資料）。 */
    private static final int MIN_CATEGORY_SALES_SAMPLE = 10;
    private static final int MIN_REVIEW_SAMPLE = 20;
    /** REVIEW_RISK risk_topic_share 的關鍵字比對表：Agent 2 ReviewRiskAgent（Track 3）尚未實作前的暫代方案。 */
    private static final Set<String> RISK_TOPIC_KEYWORDS = Set.of(
            "品質", "食安", "衛生", "過期", "發霉", "腐壞", "壞掉", "破損", "摔", "漏", "臭", "異物");

    private final ProductRepository productRepository;
    private final WeightVersionRepository weightVersionRepository;
    private final WeightProfileRepositoryPort weightProfileRepositoryPort;
    private final GradeThresholdRepositoryPort gradeThresholdRepositoryPort;
    private final RiskRuleRepositoryPort riskRuleRepositoryPort;
    private final ClimateNormalRepositoryPort climateNormalRepositoryPort;
    private final AudienceMixRepositoryPort audienceMixRepositoryPort;
    private final FestivalAffinityRepositoryPort festivalAffinityRepositoryPort;
    private final ProductScoreRepositoryPort productScoreRepositoryPort;
    private final SalesRecordRepository salesRecordRepository;
    private final ProductReviewRepository productReviewRepository;
    private final HeatCompositeDailyRepository heatCompositeDailyRepository;

    public ProductScoringOrchestrator(
            ProductRepository productRepository,
            WeightVersionRepository weightVersionRepository,
            WeightProfileRepositoryPort weightProfileRepositoryPort,
            GradeThresholdRepositoryPort gradeThresholdRepositoryPort,
            RiskRuleRepositoryPort riskRuleRepositoryPort,
            ClimateNormalRepositoryPort climateNormalRepositoryPort,
            AudienceMixRepositoryPort audienceMixRepositoryPort,
            FestivalAffinityRepositoryPort festivalAffinityRepositoryPort,
            ProductScoreRepositoryPort productScoreRepositoryPort,
            SalesRecordRepository salesRecordRepository,
            ProductReviewRepository productReviewRepository,
            HeatCompositeDailyRepository heatCompositeDailyRepository) {
        this.productRepository = productRepository;
        this.weightVersionRepository = weightVersionRepository;
        this.weightProfileRepositoryPort = weightProfileRepositoryPort;
        this.gradeThresholdRepositoryPort = gradeThresholdRepositoryPort;
        this.riskRuleRepositoryPort = riskRuleRepositoryPort;
        this.climateNormalRepositoryPort = climateNormalRepositoryPort;
        this.audienceMixRepositoryPort = audienceMixRepositoryPort;
        this.festivalAffinityRepositoryPort = festivalAffinityRepositoryPort;
        this.productScoreRepositoryPort = productScoreRepositoryPort;
        this.salesRecordRepository = salesRecordRepository;
        this.productReviewRepository = productReviewRepository;
        this.heatCompositeDailyRepository = heatCompositeDailyRepository;
    }

    /** 一次全量重評（§5.10「每週一 07:00」等全量觸發共用此方法）。 */
    @Transactional
    public BatchSummary runFullBatch(SceneResolver sceneResolver, OffsetDateTime asOf) {
        List<Product> products = productRepository.findScorable(TrackType.A);
        WeightVersion activeVersion = weightVersionRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "沒有生效中的 weight_version（is_current=true），無法評分"));
        String period = periodOf(asOf);
        LocalDate evalDate = asOf.toLocalDate();

        // ---- 第一段：整批先算出六項加分因子的原始值，不做任何正規化 ----
        Map<Long, SceneType> sceneByProduct = new LinkedHashMap<>();
        Map<Long, Map<FactorCode, RawFactor>> rawByProduct = new LinkedHashMap<>();
        for (Product product : products) {
            sceneByProduct.put(product.getId(), sceneResolver.resolve(product));
            rawByProduct.put(product.getId(), computeRawFactors(product, evalDate));
        }

        Population population = buildPopulation(products, rawByProduct);

        // ---- 第二段：母體到齊後才正規化、加權、扣分、寫入 ----
        int scored = 0;
        int skippedInsufficientData = 0;
        for (Product product : products) {
            SceneType scene = sceneByProduct.get(product.getId());
            Map<FactorCode, BigDecimal> weights =
                    weightProfileRepositoryPort.findWeights(activeVersion.getId(), scene);
            GradeThresholdSet thresholds =
                    gradeThresholdRepositoryPort.findThresholds(activeVersion.getId(), scene);

            List<ConfidencePenaltyReason> confidenceReasons = new ArrayList<>();
            List<BonusFactorInput> bonusInputs = normalizeBonusFactors(
                    product, rawByProduct.get(product.getId()), population, period, confidenceReasons);
            List<PenaltyContribution> penalties = computePenalties(product, evalDate);

            int confidence = ConfidenceCalculator.calculate(confidenceReasons);
            ScoringResult result = ScoringEngine.score(weights, bonusInputs, penalties, thresholds, confidence);

            if (!result.sufficientData()) {
                skippedInsufficientData++;
                continue;
            }

            productScoreRepositoryPort.save(
                    product.getId(), period, scene, true, activeVersion.getId(), result, confidence, asOf);
            scored++;
        }

        return new BatchSummary(products.size(), scored, skippedInsufficientData);
    }

    // ==================== 第一段：原始值蒐集 ====================

    private Map<FactorCode, RawFactor> computeRawFactors(Product product, LocalDate evalDate) {
        Map<FactorCode, RawFactor> raw = new EnumMap<>(FactorCode.class);
        raw.put(FactorCode.MARGIN, marginRaw(product));
        raw.put(FactorCode.CVR, cvrRaw(product));
        raw.put(FactorCode.PRICE_FIT, priceFitRaw(product));
        raw.put(FactorCode.FESTIVAL, festivalRaw(product, evalDate));
        raw.put(FactorCode.CLIMATE, climateRaw(product, evalDate));
        raw.put(FactorCode.TREND, trendRaw(product, evalDate));
        return raw;
    }

    /** 毛利率：{@link Product#recalculateMarginRate()} 已在寫入時算好，缺成本或售價時為 null（§5.7）。 */
    private RawFactor marginRaw(Product product) {
        BigDecimal marginRate = product.getMarginRate();
        if (marginRate == null) {
            return RawFactor.unavailable("成本或售價未填，無法計算毛利率");
        }
        return RawFactor.available(marginRate);
    }

    /**
     * 歷史轉換率（§5.2.3）：{@code conversion = Σqty / Σimpression}，四段退路依序：
     * ①本品項自身有曝光有效的紀錄 → 用自身資料，不限時間窗（規格未定義窗口）；
     * ②本品項無任何銷售紀錄，同品類 ≥ {@value #MIN_CATEGORY_SALES_SAMPLE} 筆曝光有效紀錄
     * → 品類中位數，{@code imputed = true}；
     * ③本品項有紀錄但曝光數全缺 → 以本品項紀錄涵蓋的日期範圍為「同期」，用
     * {@code qty / 品類同期平均qty}（同品類其他品項）作相對表現指標；
     * ④同品類 < {@value #MIN_CATEGORY_SALES_SAMPLE} 筆 → 無資料。
     */
    private RawFactor cvrRaw(Product product) {
        Double ownRate = salesRecordRepository.findOwnConversionRate(product.getId());
        if (ownRate != null) {
            return RawFactor.available(BigDecimal.valueOf(ownRate));
        }

        Long categoryId = product.getCategory().getId();
        List<SalesRecord> ownRecords = salesRecordRepository.findByProductId(product.getId());

        if (ownRecords.isEmpty()) {
            List<Double> categoryRatios = salesRecordRepository.findConversionRatiosByCategoryId(categoryId);
            if (categoryRatios.size() < MIN_CATEGORY_SALES_SAMPLE) {
                return RawFactor.unavailable("本品項無銷售紀錄，同品類曝光有效紀錄僅 "
                        + categoryRatios.size() + " 筆（需 ≥ " + MIN_CATEGORY_SALES_SAMPLE + " 筆）");
            }
            return RawFactor.available(median(categoryRatios), true, null);
        }

        LocalDate from = ownRecords.stream().map(SalesRecord::getOrderDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate to = ownRecords.stream().map(SalesRecord::getOrderDate).max(LocalDate::compareTo).orElseThrow();
        long ownQty = ownRecords.stream().mapToLong(SalesRecord::getQty).sum();

        List<Long> peerQty = salesRecordRepository.sumQtyByProductInCategoryAndDateRange(categoryId, from, to)
                .stream()
                .filter(p -> !p.getProductId().equals(product.getId()))
                .map(SalesRecordRepository.CategoryProductQty::getTotalQty)
                .toList();
        if (peerQty.isEmpty()) {
            return RawFactor.unavailable("曝光數全缺，且同期同品類無其他品項銷售紀錄可比較");
        }
        double avgQty = peerQty.stream().mapToLong(Long::longValue).average().orElseThrow();
        BigDecimal relative = BigDecimal.valueOf(ownQty)
                .divide(BigDecimal.valueOf(avgQty), 4, java.math.RoundingMode.HALF_UP);
        return RawFactor.available(relative,
                "曝光數缺失，改以 qty 相對品類同期（" + from + "～" + to + "）平均 qty 估算");
    }

    /** 中位數（並列採一般定義：偶數筆取中間兩數平均，不套用 §5.3.1 的百分位並列規則——那是另一套用途）。 */
    private static BigDecimal median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        java.util.Collections.sort(sorted);
        int n = sorted.size();
        double mid = (n % 2 == 1)
                ? sorted.get(n / 2)
                : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        return BigDecimal.valueOf(mid);
    }

    /** 價格帶適配：客戶未提供品類客群價格帶時，PRICE_FIT 標為無資料（R-20 降級路徑）。 */
    private RawFactor priceFitRaw(Product product) {
        if (product.getSuggestedPrice() == null) {
            return RawFactor.unavailable("尚未填寫建議售價");
        }
        List<AudienceSegmentShare> mix =
                audienceMixRepositoryPort.findMixForCategory(product.getCategory().getId());
        if (mix.isEmpty()) {
            return RawFactor.unavailable("品類客群價格帶尚未設定");
        }
        return RawFactor.available(PriceFitCalculator.priceFit(product.getSuggestedPrice(), mix));
    }

    /** 節慶時間窗：品項未關聯任何節慶，或品類前置天數未設定時標為無資料。 */
    private RawFactor festivalRaw(Product product, LocalDate evalDate) {
        List<FestivalAffinityInput> affinities =
                festivalAffinityRepositoryPort.findAffinities(product.getId(), evalDate.getYear());
        if (affinities.isEmpty()) {
            return RawFactor.unavailable("品項未關聯任何節慶");
        }
        int leadTimeDays;
        try {
            leadTimeDays = festivalAffinityRepositoryPort.findLeadTimeDays(product.getCategory().getId());
        } catch (IllegalStateException e) {
            return RawFactor.unavailable("品類前置天數未設定");
        }
        FestivalFactorResult result = FestivalWindowCalculator.compute(affinities, leadTimeDays, evalDate);
        return RawFactor.available(result.rawValue());
    }

    /** 氣候適配：僅供選品評分，用歷史同期月均溫（AC-17-4），不得用短期預報。 */
    private RawFactor climateRaw(Product product, LocalDate evalDate) {
        var idealRange = climateNormalRepositoryPort.findCategoryIdealTempRange(product.getCategory().getId());
        if (idealRange.isEmpty()) {
            return RawFactor.unavailable("品類適溫區間未設定");
        }
        var avgTemp = climateNormalRepositoryPort.findAvgTemp(DEFAULT_REGION_CODE, evalDate.getMonthValue());
        if (avgTemp.isEmpty()) {
            return RawFactor.unavailable("查無該月份歷史氣候統計");
        }
        BigDecimal fit = ClimateFitCalculator.fit(
                avgTemp.get(), idealRange.get().min(), idealRange.get().max(),
                ClimateFitCalculator.DEFAULT_TOLERANCE);
        return RawFactor.available(fit);
    }

    /**
     * 社群熱度斜率：品項可能綁多個關鍵字（§5.3.2），逐日取各關鍵字合成熱度平均值後再算斜率。
     * 不滿 7 日歷史時整個因子標為無資料；不滿 30 日則以現有最長區間計算，並標記 shortHistory。
     */
    private RawFactor trendRaw(Product product, LocalDate evalDate) {
        Set<TrendKeyword> keywords = product.getKeywords();
        if (keywords.isEmpty()) {
            return RawFactor.unavailable("品項未綁定任何關鍵字");
        }

        BigDecimal heatToday = avgCompositeAcrossKeywords(keywords, evalDate, evalDate);
        BigDecimal heatAt7d = avgCompositeAcrossKeywords(keywords, evalDate.minusDays(7), evalDate.minusDays(7));
        if (heatToday == null || heatAt7d == null) {
            return RawFactor.unavailable("熱度歷史不滿 7 日");
        }

        LocalDate windowStart = evalDate.minusDays(30);
        LocalDate windowEnd = evalDate.minusDays(8);
        OldestComposite oldest = oldestCompositeInWindow(keywords, windowStart, windowEnd);
        BigDecimal heatAtLongestWindow = oldest.value() != null ? oldest.value() : heatAt7d;
        boolean shortHistory = oldest.value() == null || oldest.date().isAfter(windowStart.plusDays(1));

        TrendSlopeResult slope = TrendSlopeCalculator.compute(heatToday, heatAt7d, heatAtLongestWindow, shortHistory);
        return RawFactor.available(slope.trendRaw(), false, slope.shortHistory() ? ConfidencePenaltyReason.SHORT_HEAT_HISTORY : null);
    }

    private BigDecimal avgCompositeAcrossKeywords(Set<TrendKeyword> keywords, LocalDate from, LocalDate to) {
        List<BigDecimal> values = new ArrayList<>();
        for (TrendKeyword keyword : keywords) {
            heatCompositeDailyRepository
                    .findByKeywordIdAndStatDateBetweenOrderByStatDateAsc(keyword.getId(), from, to)
                    .stream()
                    .findFirst()
                    .ifPresent(daily -> values.add(daily.getCompositeValue()));
        }
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 4, java.math.RoundingMode.HALF_UP);
    }

    private OldestComposite oldestCompositeInWindow(Set<TrendKeyword> keywords, LocalDate from, LocalDate to) {
        LocalDate earliestDate = null;
        List<BigDecimal> values = new ArrayList<>();
        for (TrendKeyword keyword : keywords) {
            var series = heatCompositeDailyRepository
                    .findByKeywordIdAndStatDateBetweenOrderByStatDateAsc(keyword.getId(), from, to);
            if (series.isEmpty()) {
                continue;
            }
            var earliestForKeyword = series.get(0);
            if (earliestDate == null || earliestForKeyword.getStatDate().isBefore(earliestDate)) {
                earliestDate = earliestForKeyword.getStatDate();
            }
            values.add(earliestForKeyword.getCompositeValue());
        }
        if (values.isEmpty()) {
            return new OldestComposite(null, null);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return new OldestComposite(
                sum.divide(BigDecimal.valueOf(values.size()), 4, java.math.RoundingMode.HALF_UP), earliestDate);
    }

    private record OldestComposite(BigDecimal value, LocalDate date) {
    }

    // ==================== 母體組裝（記憶體版的 §5.3.1 母體） ====================

    private Population buildPopulation(List<Product> products, Map<Long, Map<FactorCode, RawFactor>> rawByProduct) {
        Map<FactorCode, Map<Long, List<BigDecimal>>> byCategory = new EnumMap<>(FactorCode.class);
        Map<FactorCode, Map<Long, List<BigDecimal>>> byParentCategory = new EnumMap<>(FactorCode.class);
        Map<FactorCode, List<BigDecimal>> all = new EnumMap<>(FactorCode.class);
        for (FactorCode factor : FactorCode.values()) {
            if (factor.isPenalty()) {
                continue;
            }
            byCategory.put(factor, new HashMap<>());
            byParentCategory.put(factor, new HashMap<>());
            all.put(factor, new ArrayList<>());
        }

        for (Product product : products) {
            Category category = product.getCategory();
            Long categoryId = category.getId();
            Long parentId = category.getParent() == null ? null : category.getParent().getId();
            Map<FactorCode, RawFactor> raw = rawByProduct.get(product.getId());
            for (Map.Entry<FactorCode, RawFactor> entry : raw.entrySet()) {
                if (!entry.getValue().dataAvailable()) {
                    continue;
                }
                BigDecimal value = entry.getValue().rawValue();
                byCategory.get(entry.getKey())
                        .computeIfAbsent(categoryId, id -> new ArrayList<>())
                        .add(value);
                all.get(entry.getKey()).add(value);
                if (parentId != null) {
                    byParentCategory.get(entry.getKey())
                            .computeIfAbsent(parentId, id -> new ArrayList<>())
                            .add(value);
                }
            }
        }
        return new Population(byCategory, byParentCategory, all);
    }

    // ==================== 第二段：正規化 + 扣分 ====================

    private List<BonusFactorInput> normalizeBonusFactors(
            Product product, Map<FactorCode, RawFactor> raw, Population population, String period,
            List<ConfidencePenaltyReason> confidenceReasons) {

        Long categoryId = product.getCategory().getId();
        Long parentId = product.getCategory().getParent() == null
                ? null : product.getCategory().getParent().getId();

        List<BonusFactorInput> inputs = new ArrayList<>();
        for (FactorCode factor : FactorCode.values()) {
            if (factor.isPenalty()) {
                continue;
            }
            RawFactor factorRaw = raw.get(factor);
            if (!factorRaw.dataAvailable()) {
                confidenceReasons.add(ConfidencePenaltyReason.PER_MISSING_FACTOR);
                inputs.add(BonusFactorInput.unavailable(factor, factorRaw.note()));
                continue;
            }
            if (factorRaw.trendPenaltyReason() != null) {
                confidenceReasons.add(factorRaw.trendPenaltyReason());
            }
            if (factorRaw.imputed()) {
                confidenceReasons.add(ConfidencePenaltyReason.PER_IMPUTED_FACTOR);
            }

            List<BigDecimal> ownPop = population.byCategory().get(factor).getOrDefault(categoryId, List.of());
            List<BigDecimal> siblingPop = parentId == null
                    ? List.of()
                    : population.byParentCategory().get(factor).getOrDefault(parentId, List.of());
            List<BigDecimal> allPop = population.all().get(factor);

            NormalizationResult normalized =
                    PercentileNormalizer.normalize(factorRaw.rawValue(), ownPop, siblingPop, allPop);
            if (normalized.confidencePenaltyReason() != null) {
                confidenceReasons.add(normalized.confidencePenaltyReason());
            }
            String note = normalized.note() != null ? normalized.note() : factorRaw.note();
            inputs.add(new BonusFactorInput(
                    factor, factorRaw.rawValue(), normalized.percentile(), true, factorRaw.imputed(), note));
        }
        return inputs;
    }

    private List<PenaltyContribution> computePenalties(Product product, LocalDate evalDate) {
        List<PenaltyContribution> penalties = new ArrayList<>();
        penalties.add(reviewRisk(product));
        penalties.add(logisticsRisk(product, evalDate));
        penalties.add(inventoryRisk(product));
        return penalties;
    }

    /**
     * REVIEW_RISK（§5.2.2）。{@code risk_topic_share} 本應由 Agent 2 ReviewRiskAgent
     * （Track 3，尚未實作）分類；在那之前用 {@link #RISK_TOPIC_KEYWORDS} 對
     * {@code review_analysis.key_phrase} 做關鍵字比對，屬暫代方案，Track 3 完工後應取代。
     */
    private PenaltyContribution reviewRisk(Product product) {
        long total = productReviewRepository.countByProductId(product.getId());
        if (total < MIN_REVIEW_SAMPLE) {
            return ReviewRiskCalculator.calculate(0, (int) total, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        long negative = productReviewRepository.countByProductIdAndAnalysisSentiment(
                product.getId(), Sentiment.NEGATIVE);

        BigDecimal categoryThreshold = reviewRiskThreshold(product.getCategory().getId());
        BigDecimal riskTopicShare = BigDecimal.ZERO;
        if (negative > 0) {
            List<String> keyPhrases = productReviewRepository.findKeyPhrasesByProductIdAndAnalysisSentiment(
                    product.getId(), Sentiment.NEGATIVE);
            long risky = keyPhrases.stream()
                    .filter(phrase -> phrase != null && RISK_TOPIC_KEYWORDS.stream().anyMatch(phrase::contains))
                    .count();
            riskTopicShare = BigDecimal.valueOf(risky)
                    .divide(BigDecimal.valueOf(keyPhrases.size()), 4, java.math.RoundingMode.HALF_UP);
        }
        return ReviewRiskCalculator.calculate((int) negative, (int) total, categoryThreshold, riskTopicShare);
    }

    /**
     * {@code risk_rule.threshold_json} 的 key 名稱（{@code negative_rate_threshold}）為本次新定義，
     * 資料庫尚無對應 seed（migration 中找不到既有 risk_rule 資料）——SYS_ADMIN 需補上這筆設定，
     * 否則 {@link RiskRuleRepositoryPort#findConfig} 會直接丟例外（找不到規則）。
     */
    private BigDecimal reviewRiskThreshold(Long categoryId) {
        Map<String, BigDecimal> thresholds =
                riskRuleRepositoryPort.findConfig("REVIEW_RISK", categoryId).thresholds();
        BigDecimal threshold = thresholds.get("negative_rate_threshold");
        if (threshold == null) {
            throw new IllegalStateException(
                    "risk_rule(REVIEW_RISK) 的 threshold_json 缺少 negative_rate_threshold");
        }
        return threshold;
    }

    private PenaltyContribution logisticsRisk(Product product, LocalDate evalDate) {
        Set<LogisticsCondition> conditions = parseLogisticsConditions(product.getLogisticsCondition());
        return LogisticsRiskCalculator.calculate(conditions, evalDate.getMonth());
    }

    private Set<LogisticsCondition> parseLogisticsConditions(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        Set<LogisticsCondition> conditions = java.util.EnumSet.noneOf(LogisticsCondition.class);
        for (String token : csv.split(",")) {
            conditions.add(LogisticsCondition.valueOf(token.trim()));
        }
        return conditions;
    }

    private PenaltyContribution inventoryRisk(Product product) {
        return InventoryRiskCalculator.calculate(product.getShelfLifeDays(), product.getSeason(), product.getMoq());
    }

    /** ISO 週字串（如 2026W30），以 Asia/Taipei 判定週界（§7.2.6）。 */
    static String periodOf(OffsetDateTime asOf) {
        LocalDate taipeiDate = asOf.atZoneSameInstant(TAIPEI).toLocalDate();
        int weekBasedYear = taipeiDate.get(IsoFields.WEEK_BASED_YEAR);
        int week = taipeiDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return "%dW%02d".formatted(weekBasedYear, week);
    }

    private record Population(
            Map<FactorCode, Map<Long, List<BigDecimal>>> byCategory,
            Map<FactorCode, Map<Long, List<BigDecimal>>> byParentCategory,
            Map<FactorCode, List<BigDecimal>> all) {
    }

    private record RawFactor(
            BigDecimal rawValue, boolean dataAvailable, boolean imputed, String note,
            ConfidencePenaltyReason trendPenaltyReason) {

        static RawFactor available(BigDecimal value) {
            return new RawFactor(value, true, false, null, null);
        }

        static RawFactor available(BigDecimal value, boolean imputed, ConfidencePenaltyReason reason) {
            return new RawFactor(value, true, imputed, null, reason);
        }

        static RawFactor available(BigDecimal value, String note) {
            return new RawFactor(value, true, false, note, null);
        }

        static RawFactor unavailable(String note) {
            return new RawFactor(null, false, false, note, null);
        }
    }

    public record BatchSummary(int totalCandidates, int scored, int skippedInsufficientData) {
    }
}
