-- ===================================================================
-- V22：risk_rule 既有 3 筆全域列，格式對齊 §5.2.2 v3.0 模型
-- ===================================================================
-- 本機資料（遠端 pg_dump 鏡像）帶著 REVIEW_RISK／LOGISTICS_RISK／INVENTORY_RISK
-- 三筆全域列，但 threshold_json 是 v2.0 舊格式：
--   - REVIEW_RISK：{"minSampleSize":5,"negativeRateThreshold":0.15}，max_penalty 15
--   - LOGISTICS_RISK：{"conditions":[...]}（單一命中即扣滿分，無逐條件點數），max_penalty 12
--   - INVENTORY_RISK：{"moqThreshold":300,"shelfLifeDaysThreshold":30}，max_penalty 13
-- §5.2.2 原文已說明這正是 v2.0 「只給上限與觸發條件文字描述」的樣子，v3.0 定義了
-- 逐條件加總的公式形狀與初始點數（待客戶確認，附錄 A 第 14、18 項）。CLAUDE.md
-- 「spec 與現況衝突時 spec 贏」——這裡改用 UPDATE 對齊既有列，不用 INSERT
-- （會撞 uk_risk_rule 唯一鍵），也不動 category_id=12 的 INVENTORY_RISK 品類覆寫列
-- （品類覆寫是否沿用同一套 v2.0 門檻待 SYS_ADMIN 另行確認，非本次範圍）。
--
-- key 名稱對應 ReviewRiskCalculator／LogisticsRiskCalculator.Points／
-- InventoryRiskCalculator.Thresholds 讀取的欄位（ssds-api ProductScoringOrchestrator）。
-- max_penalty 改回 FactorCode 的結構性上限（REVIEW_RISK 20、LOGISTICS_RISK／
-- INVENTORY_RISK 10，§5.2）。

UPDATE risk_rule
SET threshold_json = '{"negative_rate_threshold": 0.15}'::jsonb,
    max_penalty     = 20,
    updated_at      = now()
WHERE rule_code = 'REVIEW_RISK' AND category_id IS NULL;

UPDATE risk_rule
SET threshold_json = '{"meltable_summer_points": 4, "cold_chain_points": 4, "fragile_points": 3, "oversized_points": 3}'::jsonb,
    max_penalty     = 10,
    updated_at      = now()
WHERE rule_code = 'LOGISTICS_RISK' AND category_id IS NULL;

UPDATE risk_rule
SET threshold_json = '{"short_shelf_life_days": 60, "short_shelf_life_points": 4, "seasonal_points": 3, "high_moq": 300, "high_moq_points": 3}'::jsonb,
    max_penalty     = 10,
    updated_at      = now()
WHERE rule_code = 'INVENTORY_RISK' AND category_id IS NULL;
