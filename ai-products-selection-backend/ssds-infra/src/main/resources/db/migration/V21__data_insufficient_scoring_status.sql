-- ===================================================================
-- V21：落地 §5.7「資料不足，無法評分」的機制（規格書 L1971-1978）
-- ===================================================================
-- product_score.grade／final_score 是 NOT NULL（§7.2.6），資料不足時不能靠
-- 寫一筆「空評分」代表無法評分，否則 §FR-04 四榜的排行查詢會讓該品項直接
-- 消失、沒有任何「明確標示」（與 FR-04 設計要點矛盾）。規格書 L1977-1978
-- 訂的落地方式：
--   1. product 新增 last_scoring_status／last_scoring_attempted_at 兩欄，
--      每次評分嘗試（全量或單筆）結束時寫入，供 §FR-03／§FR-05 查詢，
--      不需 join product_score。
--   2. 產生一筆 risk_type = 'DATA_INSUFFICIENT' 的 risk_alert，比照
--      PENALTY_CAP 一樣主動推進風險示警清單。

ALTER TABLE product
    ADD COLUMN last_scoring_status VARCHAR(20) NULL
        CHECK (last_scoring_status IN ('SCORED', 'INSUFFICIENT_DATA')),
    ADD COLUMN last_scoring_attempted_at TIMESTAMPTZ NULL;

COMMENT ON COLUMN product.last_scoring_status IS
    '最近一次評分嘗試的技術結果，與 status 的採購業務狀態機分開維護（§5.7 落地機制）。NULL 表尚未嘗試過評分';
COMMENT ON COLUMN product.last_scoring_attempted_at IS
    '供 §FR-03 品項清單、§FR-05 詳情頁查詢是否需標示「資料不足」，不需 join product_score';

-- risk_alert.risk_type 值域補上 DATA_INSUFFICIENT（V17 的 9 個值漏掉這個
-- v3.0 新增值；畫面示意圖已在 §4079 那次 commit 同步過，DB 端本次才補）。
ALTER TABLE risk_alert
    DROP CONSTRAINT ck_risk_alert_type;

ALTER TABLE risk_alert
    ADD CONSTRAINT ck_risk_alert_type
        CHECK (risk_type IN (
            'REVIEW_RISK', 'LOGISTICS_RISK', 'INVENTORY_RISK', 'PENALTY_CAP',
            'HEAT_CRASH', 'HEAT_SURGE', 'SEASON_MISMATCH',
            'FESTIVAL_WINDOW_CLOSING', 'LOW_CONFIDENCE', 'DATA_INSUFFICIENT'));
