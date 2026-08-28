# 模組任務清單（依 開發規格書_v3.0.md 拆解）

依據 §3.3 模組結構 + §12.1 範圍分層 + §4 功能需求(FR) 拆分。標記：
**M=必做**（驗證核心價值）、**O=次要**（時程允許再做）。

## 依賴關係（決定平行開發順序）

```
                    ssds-api        （組裝層，最後做）
        ┌──────────────┼──────────────┬──────────────┐
   ssds-infra      ssds-ai      ssds-ingest   ssds-calibration
        │              │              │              │
        └──────────────┴──────┬───────┴──────────────┘
                              ▼
                          ssds-core   （無依賴，最先做）
```

`ssds-core` 不依賴任何模組，`ssds-infra` / `ssds-ai` / `ssds-ingest` / `ssds-calibration` 互相不依賴（都只依賴 core）。`ssds-api` 依賴全部，必須最後組裝。

---

## Phase 0（序列，禁止平行）— 契約與骨架

這階段不能拆 worktree 平行做，因為後面所有模組都要吃這裡定案的契約。

| 任務 | 內容 | 對應章節 |
|---|---|---|
| DB schema 定案 | §7.2 全部資料表 + Flyway V1 腳本 | §7 |
| `ssds-core` 領域模型 + `port/` 介面 | Repository 介面（給 infra 實作）、Product/Score/WeightProfile 等領域物件 | §3.3, §7.2 |
| OpenAPI 契約 v1 骨架 | 依 §8.2 端點清單產出 `openapi.json` | §8, §3.3 契約管理 |
| 角色權限矩陣落地 | §2.1 權限矩陣轉成 Spring Security 設定骨架 | §2.1 |

---

## Phase 1（可平行，各自 worktree）

Phase 0 契約凍結後，以下五條線互不依賴，可各開一個 git worktree / branch 平行進行。

### Track 1 — `ssds-core`（評分引擎，TDD 主戰場）**[M]**
- 六個加分因子計算 + 三個扣分規則（§5.2）
- 同品類正規化（§5.3）
- 加權公式、加分小計/扣分小計（§5.5）— **§11.1 黃金案例測試直接拿來當測試案例**
- 分級規則 A/B/C（§5.6）
- 資料不足處理 / 權重分攤（§5.7）
- B 軌時效落差模型（§5.8）**[O，降級：僅清單+計算]**
- 信心度（§5.9）
- 節慶時間窗、氣候基準（§5, §FR-17）**[O，氣候因子可停用]**
- 品項狀態機（§7.4）

### Track 2 — `ssds-infra`
- Entity + Repository 實作（落地 core 的 port 介面）
- Flyway migration 維護
- 排程（趨勢每日更新、風險示警排程 §FR-10）
- 快取

### Track 3 — `ssds-ai`（opencode + OpenRouter）
- AiOrchestrator、PromptSanitizer（§6.5, §6.6）
- Agent 1 SceneClassifier **[M，情境判定]**
- Agent 2 ReviewRisk、Agent 3 ProductInsight、Agent 4 Recommendation **[M，§FR-05 AI 分析]**
- Agent 5 TrendInterpreter **[M，配合 FR-06]**
- Agent 6 SourcingScout **[O，B 軌，§FR-16]**
- Agent 7 WeightCalibration **[O，§FR-15]**
- Schema 驗證、四層降級、任務中心（§FR-07）
- 預算池與模型分派（§6.7）

### Track 4 — `ssds-ingest`
- Adapter：Threads、Google Trends **[M，§FR-06/14-2]**；Instagram **[O]**
- 人工標記來源 adapter（§FR-14-1）**[M]**
- composite/ 多來源合成與斜率計算 **[M]**
- importer/ CSV/XLSX 匯入（銷售、評論、會員輪廓、品項主檔，§FR-09）

### Track 5 — `ssds-frontend`（Angular，獨立 repo/worktree）
- 版面骨架、路由表（§9.1, §9.2）
- FR-01 登入、FR-02 儀表板、FR-03 品項管理 **[M，對應 P1 基礎]**
- FR-04 選品分數排行（四榜）**[M]**
- FR-05 品項詳情與 AI 分析（含加扣分透明化）**[M]**
- FR-06 趨勢分析、FR-08 情境權重組設定 **[M]**
- FR-14-1 人工熱度標記介面 **[M]**
- FR-10 風險示警中心、FR-11 決策與回饋閉環 **[M]**
- FR-07 AI 任務中心、FR-09 資料匯入、FR-12 報表、FR-13 系統設定、FR-16 尋源探索、FR-17 節慶維護、FR-18 稽核紀錄 **[O，依序補]**
- 以 openapi-generator 產生 TS client，禁止手刻重複型別

`ssds-calibration`（第 6 條線，可與上面一起平行）：
- 統計迴歸、歷史回測 **[M，降級版：僅回測不做線上校準]**
- 校準報告、AI 解讀串接（依賴 ssds-ai）**[O]**

---

## Phase 2（序列）— 組裝與整合

- `ssds-api`：Controller、DTO、Security 設定、例外處理，組裝 Phase 1 所有模組的 Spring context
- 跨模組整合測試（§11.2, §11.3 E2E 六條主流程）
- 前後端聯調（依 openapi.json 契約）

---

## 進度與已知問題（2026-08-27）

**已完成**
- Phase 0：DB schema（Flyway V1~V20，對齊 §7.2）、前端路由骨架。
- FR-01 登入與權限（後端）：`AuthService`／`JwtTokenProvider`／`SecurityConfig` 等，見 `ai-products-selection-backend/ssds-api/src/main/java/com/example/ssds/api/security/`。`/auth/login`、`/auth/refresh`、`/auth/logout`、`/auth/me` 四支端點皆已實作並對真實共用 DB 驗證過（密碼錯誤／帳號停用／缺 token 三條路徑）。角色層級的存取控制（AC-01-3／AC-01-4）走 `@PreAuthorize`，目前尚無受保護的寫入端點可掛，等對應 FR 開發時再加。
- Track 1 `ssds-core/scoring` + `port/`（評分引擎，§5 全部公式）：`com.example.ssds.core.scoring` 下的純函式計算器（`ScoringEngine`／`WeightAllocator`／`GradeClassifier`／`ConfidenceCalculator`／`PercentileNormalizer`／`TrendSlopeCalculator`／`HeatCompositeCalculator`／`FestivalWindowCalculator`／`ClimateFitCalculator`／`PriceFitCalculator`／`ReviewRiskCalculator`／`LogisticsRiskCalculator`／`InventoryRiskCalculator`／`HeatVolumeGate`），零 JPA 依賴、可脫離資料庫單元測試。`com.example.ssds.core.port` 下 8 支介面（`WeightProfileRepositoryPort`、`GradeThresholdRepositoryPort`、`RiskRuleRepositoryPort`、`CategoryPercentilePopulationPort`、`HeatCompositeRepositoryPort`、`FestivalAffinityRepositoryPort`、`ClimateNormalRepositoryPort`、`AudienceMixRepositoryPort`、`ProductScoreRepositoryPort`）供 `ssds-infra` 之後實作（依賴反轉）。§11.1 黃金案例（86.89／4.00／82.89／B／confidence 86，及 11 月變體 90.69／A）與各因子公式的個別黃金驗算（§5.2.4 PRICE_FIT 0.682、§FR-17-2 CLIMATE 0.717、§FR-17-1 FESTIVAL 0.60 + 邊界值）共 13 個測試類別、40 個測試，`./gradlew :ssds-core:test` 全綠。
  - **`LOGISTICS_RISK`／`INVENTORY_RISK` 的點數表為佔位值**：規格書 §5.2.2 只給出扣分上限（各 10）與黃金案例的單一結果（夏季+易融化→4；效期 180 天/MOQ 200→0），未定義每個條件的確切點數（不像 `REVIEW_RISK` 有完整公式）。已用 golden case 反推出一組自洽的預設值並在程式碼註解說明，性質等同附錄 A 的「待客戶確認」項目，正式門檻應由 SYS_ADMIN 透過 `risk_rule.threshold_json` 校準覆寫（`RiskRuleRepositoryPort` 已預留此介面）。
  - **尚未串接 `ssds-infra`**：port 介面目前無任何實作，也沒有從 DB 讀資料組出 `BonusFactorInput`／`HeatSourceContribution` 等輸入的組裝層（例如 `ProductScoringOrchestrator`）。這是 Phase 2（`ssds-api` 組裝）或 Track 2（`ssds-infra`）的工作，而且 `audience_segment`／`category_audience_mix`／`grade_threshold`／`risk_rule`／`heat_composite_daily`／`category_climate_profile` 這幾張 v3.0 新增表目前在 `ssds-infra/entity` 完全沒有對應 Entity/Repository（見下方已知問題 3）。

**已知問題（尚未修，使用者已表示資料庫與依賴先不動）**
1. **`V900__seed_master_data.sql` 的種子密碼對不上**：註解宣稱明碼是 `Ssds@2026`，但實測所有帳號（`buyer@ssds.dev` 等）用這組密碼登入皆失敗，雜湊值與該明碼不匹配。目前沒有任何已知明碼能登入種子帳號。修法：重新產生正確的 BCrypt hash，開一支新的 dev migration（如 `V901`）覆蓋，而不是改 V900（已套用過，改了會讓 Flyway checksum 對不上）。
2. **`ssds-infra` 測試編譯失敗**：`MigrationVerificationTest.java` 用了 Testcontainers（`org.testcontainers.*`），但 `ssds-infra/build.gradle` 沒宣告這個測試依賴，`./gradlew build`（不加 `-x test`）會在 `:ssds-infra:compileTestJava` 失敗。這是既有問題，不是 FR-01 改動造成的。跑 `./gradlew build -x test` 或針對個別模組 `:ssds-api:test` / `:ssds-core:test` 可繞開。
3. ~~`ssds-infra` 缺少評分引擎需要的六張 v3.0 新增表的 Entity/Repository~~ **已於本次（2026-08-27）補上**，見下方 Track 2 進度。

**Track 2 進度（2026-08-27）：`ssds-infra` 補齊評分引擎所需的 Entity/Repository + port 實作**
- 新增 6 個 Entity + Repository（對照本機還原的真實 schema逐欄核對）：`AudienceSegment`、`CategoryAudienceMix`（複合鍵 `CategoryAudienceMixId`）、`GradeThreshold`（複合鍵 `GradeThresholdId`）、`RiskRule`（`threshold_json` 存 `String`，同 `AiInsight.contentJson` 的既有慣例，不反序列化成物件）、`HeatCompositeDaily`（複合鍵 `HeatCompositeDailyId`）、`CategoryClimateProfile`（與 `Category` 1:1 共用主鍵，同 `CategoryLeadTime` 寫法）。
- 補上三個既有 Entity 缺欄位的落差（皆已用本機 DB 的 `\d` 輸出核對過欄位存在，非猜測）：`ScoreFactor` 缺 `note`（§7.2.6 有此欄，AC-05 的因子註記要用）；`HeatReading` 的 `keyword` 寫死 `optional=false`、且完全沒有 `category` 欄位，導致 Instagram 這種品類級來源（§FR-06、§5.3.2）存不進去；`HeatSource` 缺 `granularity`，沒有它就套不了 §5.3.2 的品類級 0.5 折扣。
- `ssds-core/port` 8 支介面中的 7 支已在 `ssds-infra/port` 實作（`*RepositoryPortAdapter`，`@Component`）：`WeightProfileRepositoryPort`、`GradeThresholdRepositoryPort`、`RiskRuleRepositoryPort`、`ClimateNormalRepositoryPort`、`AudienceMixRepositoryPort`、`FestivalAffinityRepositoryPort`、`HeatCompositeRepositoryPort`、`ProductScoreRepositoryPort`（寫入路徑；同鍵重複評分時會先把舊的 `is_active` 現行列改 false 再寫新列，符合 §5.10）。
- 順手修正 `ssds-core` 的 port 設計缺口：`ProductScoreRepositoryPort.save()` 原本沒有 `confidence` 參數（`ScoringResult` 本來就不含信心度，是 `ConfidenceCalculator` 另外算的），且沒有擋「資料不足時不該寫 product_score 列」——`grade` 欄位 DB 端是 NOT NULL，若把 `sufficientData=false` 的結果硬寫進去會在 flush 時才炸。現在 adapter 對此直接丟 `IllegalArgumentException`，逼呼叫端在呼叫前過濾。
- **`CategoryPercentilePopulationPort` 已實作**（`CategoryPercentilePopulationPortAdapter`），母體查詢自 `score_factor.raw_value`（同 period、同品類，只取 `data_available=true` 的列；`ScoreFactorRepository` 新增三支對應 `@Query`）。**語意上有個必須遵守的隱含順序，寫在該 adapter 的 class Javadoc 裡**：母體是「同一 period 內其他品項這個因子的原始值」，所以全量重評必須分兩段跑——第一段把該批次全部品項的原始值先寫進 `score_factor`（`normalized_value` 可先留 null），第二段才呼叫本介面做正規化；不能一邊算一邊逐品項正規化，否則批次裡最先算的品項會拿到不完整的母體。這個兩段式順序由呼叫端（評分批次編排）保證，port 本身無法強制。
  - 順手修正 port 介面本身的型別錯誤：`CategoryPercentilePopulationPort` 原本簽章是 `LocalDate period`，但系統的 period 概念是 ISO 週字串（`product_score.period`，如 `2026W30`），不是日期——寫介面時想成了「某一天」，接上 `ScoreFactorRepository` 才發現對不上，已改為 `String period`。
- 驗證方式：`./gradlew build -x test` 全模組編譯過；`SPRING_PROFILES_ACTIVE=local` 對本機 DB 跑 `bootRun`，Hibernate `ddl-auto=validate` 通過。`ssds-infra` 尚無新增測試——既有的 `MigrationVerificationTest` 編譯失敗（已知問題 2）擋住了整個模組的測試編譯，新 adapter 要嘛等該問題解決、要嘛改用 Mockito 純單元測試繞過，本次未做。

`ssds-core/port` 8 支介面**全部**已在 `ssds-infra/port` 實作完畢。

下一步照 §12.1／本檔 Phase 1 規劃：Track 1（`ssds-core/scoring`）與 Track 2 的資料存取層（含全部 port 實作）已完成，下一個關鍵路徑是 Phase 2 的評分批次編排——一個串起所有 port + calculator 的 orchestrator（跑在 `ssds-api` 或新的 service 層），把 §5.10 的觸發時機表接上排程／API 觸發點，並落實上面兩段式批次順序。這是目前唯一還沒人碰過的整合層，其餘 track（AI／熱度資料層／前端）可平行推進，但最終串接評分結果都要經過這一層。

## 本機開發資料庫（2026-08-27）

改用本機 Docker Postgres 開發，不再直接連共用 Supabase。設定與操作見 `CLAUDE.md`「Backend」一節；重點：
- `ai-products-selection-backend/docker-compose.yml`：`postgres:17-alpine`（對齊遠端 17.6），`docker compose up -d` 啟動。
- 本機資料是遠端的一次性鏡像（`pg_dump --data-only` 撈回，schema 由 `db/migration` 19 支腳本重新套用），47 張表、真實列數（如 `product` 31 筆、`heat_composite_daily` 1850 筆），非即時同步。
- `.env` 已切換為本機連線，`SPRING_PROFILES_ACTIVE=local`（**不可用預設的 `dev`**——`dev` 會多套 `db/dev` 的種子 migration V900+，跟已還原的真實資料主鍵衝突，此為實測結果非推測）。
- 遠端連線設定備份於 `ai-products-selection-backend/.scratch/env.remote.backup`（已 gitignore），要切回遠端時複製覆蓋 `.env` 即可。

## Session 結束註記（2026-08-27）

**本次 session 完成的事，已全部 commit（4 個 commit，`master` 分支）**

1. `25cb139` — `ssds-core/scoring` 評分引擎：§5 全部公式（加權、分級、信心度、正規化、熱度斜率/合成、節慶時間窗、氣候適配、價格帶適配、三個扣分規則）+ 8 支 port 介面。40 測試全綠，含 §11.1 黃金案例。
2. `ea5bf4d` — `ssds-infra`：8 支 port **全部**實作、補 6 張表的 Entity/Repository、修 3 個既有 Entity 的欄位缺口（`ScoreFactor.note`、`HeatReading.category`、`HeatSource.granularity`）。
3. `07915a6` — 本機 Docker Postgres 開發環境（`docker-compose.yml`），資料為遠端 pg_dump 一次性鏡像，`.env` 已切換。
4. `ef2764f` — 規格書 `開發規格書_v3.0.md` 補三處缺口（§5.2.2 `LOGISTICS_RISK`/`INVENTORY_RISK` 公式、§5.7/§7.2 資料不足落地機制、§5.10 全量重評批次順序），並同步修正 `畫面功能示意圖_v3.0.html` 的 risk_type 計數（9→10）。

**下一步要做的事，依優先序**

1. ~~評分批次編排（最優先，Phase 2 關鍵路徑）~~ **已於本次（2026-08-27）完成**，見下方「Phase 2 進度」。
2. ~~落實「資料不足」的 schema 變更~~ **已於本次（2026-08-27）完成**：新增 `V21__data_insufficient_scoring_status.sql`——`product` 加 `last_scoring_status`（`SCORED`/`INSUFFICIENT_DATA`，對應新 domain enum `com.example.ssds.core.domain.LastScoringStatus`）與 `last_scoring_attempted_at` 兩欄；`risk_alert.risk_type` 的 `ck_risk_alert_type` CHECK 補上 `DATA_INSUFFICIENT`（V17 的 9 個值漏掉這個 v3.0 新增值）。`ProductScoringOrchestrator` 已接上：每次評分嘗試（成功或資料不足）結束都寫 `Product.lastScoringStatus`／`lastScoringAttemptedAt`（`markScoringAttempt`）；資料不足時額外開一筆 `DATA_INSUFFICIENT` 的 `risk_alert`（`raiseDataInsufficientAlert`，用 `existsByProductIdAndRiskTypeAndStatus` 去重，比照 `PENALTY_CAP` 主動推進示警清單，§5.7、§FR-10-1）。`:ssds-api:test`（含全 context 啟動、本機 DB 跑過 Flyway V21）、`:ssds-core:test` 全綠。
3. ~~`LOGISTICS_RISK`／`INVENTORY_RISK` 改吃 `RiskRuleRepositoryPort`~~ **已於本次（2026-08-27）完成**：兩個 calculator 的簽章改吃 `LogisticsRiskCalculator.Points`／`InventoryRiskCalculator.Thresholds`（純 record，不依賴 port，維持 calculator 無副作用），原本寫死的點數/門檻常數全部移除；`ProductScoringOrchestrator.logisticsPoints`／`inventoryThresholds` 向 `RiskRuleRepositoryPort.findConfig("LOGISTICS_RISK"/"INVENTORY_RISK", categoryId)` 取值並用共用的 `requireThreshold` 解析，缺 key 直接丟例外（同 `REVIEW_RISK` 既有作法）。因子結構性上限（10 分）仍取自 `FactorCode.maxPenalty()`，不下放給 `risk_rule`——那是 §5.2 架構常數，不是 SYS_ADMIN 校準對象。`ssds-core` 的 `LogisticsAndInventoryRiskCalculatorTest`、`ssds-api` 的 `ProductScoringOrchestratorTest` 皆已改用新簽章，`:ssds-api:test :ssds-core:test` 全綠。
4. ~~補 `risk_rule` seed data~~ **已於本次（2026-08-28）完成**，見下方「`risk_rule` 資料對齊 v3.0」。三個扣分規則現在都有可查詢的門檻設定，`ProductScoringOrchestrator` 理論上可對本機 DB 端到端跑一次全量評分（尚未實際排程/手動觸發驗證過，見下方「接下來」）。
5. 已知問題 1、2（種子密碼、`MigrationVerificationTest` 編譯失敗）仍未修，使用者先前表示資料庫與依賴先不動，維持現狀。
6. Track 3（AI Agent）／Track 4（熱度資料層）／前端可平行推進，不受上述影響，但最終串接評分結果都要經過第 1 點的 orchestrator。

## Phase 2 進度（2026-08-27）：評分批次編排（`ProductScoringOrchestrator`）

新增 `com.example.ssds.api.scoring.ProductScoringOrchestrator`（`ssds-api/src/main/java/.../api/scoring/`），第一次把 Track 1 的計算器與 Track 2 的 8 支 port 串成一次真正的評分。`runFullBatch(SceneResolver, OffsetDateTime)` 是唯一入口，尚未接上排程／API 觸發點（§5.10 的觸發時機表本次未做，留給下一步）。

- **情境判定**：新增 `SceneResolver`（`api/scoring/SceneResolver.java`）函式介面，`SceneType resolve(Product)`。Agent 1 SceneClassifierAgent（Track 3）尚未實作，本次刻意不放任何假 LLM 呼叫或寫死規則——呼叫端目前得自行提供情境（如全部丟 `REPLENISHMENT`），Track 3 完工後換一個真的實作即可，orchestrator 不必改。
- **§5.10 兩段式順序**：因為 `product_score.grade` 目前是 DB NOT NULL（見上方「下一步」第 2 點），批次內任何品項在算完六項因子前都無法先寫一筆「未完成」的 `product_score`／`score_factor`，因此 `CategoryPercentilePopulationPort`（讀 DB 現有 `score_factor.raw_value`）在全量批次的當下用不上——它仍是對的介面，但只適用於「母體已由前一批寫好」的情境（如單筆評分）。本次改為批次全部品項的原始值先留在記憶體（`computeRawFactors` → `buildPopulation`），母體到齊後才對每個品項正規化並一次寫入（`ProductScoreRepositoryPort#save`）。等 schema 缺口補上後，可以改成真的分兩次交易寫 DB，屆時全量批次也能直接用 `CategoryPercentilePopulationPort`。
- **六項加分因子的原始值來源**：
  - `MARGIN`：直接讀 `Product.marginRate`（已由既有的 `recalculateMarginRate()` 維護）。
  - `CVR`：**2026-08-27 修正**——規格書 §5.2.3 其實已完整定義此因子（Σqty/Σimpression + 四段退路），先前誤植為「規格未定義、暫採 90 天窗口」，屬審視 code 對照規格時發現的實作錯誤，非規格缺口。已改為 `SalesRecordRepository#findOwnConversionRate`（本品項自身、不限時間窗）→ 無自身紀錄時查 `findConversionRatiosByCategoryId` 取同品類 ≥10 筆的中位數（`imputed=true`）→ 有自身紀錄但曝光數全缺時以自身紀錄涵蓋日期範圍為「同期」，用 `sumQtyByProductInCategoryAndDateRange` 算 `qty / 品類同期平均qty` 相對指標 → 同品類 < 10 筆則無資料，四段全依 §5.2.3 原文順序。同時發現 `imputed=true` 的因子先前完全沒有觸發 `ConfidencePenaltyReason.PER_IMPUTED_FACTOR`（§5.9 信心度扣分漏了這一項，`normalizeBonusFactors` 已補上），這是連帶修正、非 CVR 專屬。
  - `PRICE_FIT`：`AudienceMixRepositoryPort` + `Product.suggestedPrice`，客群價格帶未設定或售價未填則無資料。
  - `FESTIVAL`：`FestivalAffinityRepositoryPort`，品項未關聯節慶或品類前置天數未設定則無資料。
  - `CLIMATE`：`ClimateNormalRepositoryPort`，地區碼**固定寫死 `"TW_TPE"`**（2026-08-27 由 `"TW"` 修正——附錄 B `CLIMATE_REGION_DEFAULT` 與 §7.2 `region_code` 皆為 `TW_TPE`，先前寫死的值本身就不合規格，非待確認項）——`product`／`category` 目前都沒有地區欄位，先以此預設值為準（§FR-17-2 待補區域化）。
  - `TREND`：直接用 `HeatCompositeDailyRepository`（略過 `HeatCompositeRepositoryPort`，因為該 port 只查單日單一 keyword，斜率需要一段區間；`ssds-api` 依模組圖本就可以直接依賴 `ssds-infra`）。品項可能綁多個關鍵字，逐日取各關鍵字合成熱度的平均值再算斜率；不滿 7 日歷史整個因子標無資料，不滿 30 日則以現有最長區間計算並標記 `SHORT_HEAT_HISTORY`。
- **三個扣分規則**：`REVIEW_RISK`／`LOGISTICS_RISK`／`INVENTORY_RISK` 皆已接 `RiskRuleRepositoryPort.findConfig(ruleCode, categoryId)`（`LOGISTICS_RISK`／`INVENTORY_RISK` 於 2026-08-27 稍後補上，見下方「下一步」第 3 點；三筆的 `risk_rule` seed 於 2026-08-28 補齊，見下方「`risk_rule` 資料對齊 v3.0」）。門檻 key 名稱（`negative_rate_threshold` 等）皆為本次新定義。`risk_topic_share`（負評中屬品質／食安／物流破損的比例）本應由 Agent 2 ReviewRiskAgent（Track 3）分類，Track 3 未實作前改用關鍵字比對 `review_analysis.key_phrase`（`ProductScoringOrchestrator.RISK_TOPIC_KEYWORDS`），屬暫代方案。
- **`ProductReviewRepository` 新增兩支查詢方法**（無 schema 變更）：`countByProductIdAndAnalysisSentiment`、`findKeyPhrasesByProductIdAndAnalysisSentiment`，供上述 `REVIEW_RISK` 使用。
- **`ssds-api/build.gradle` 補了 `testImplementation libs.test.spring.boot`**：webmvc/security/validation 三支 test starter 不保證帶 Mockito，`ssds-core` 也是靠這支測試依賴。
- 測試：`ssds-api/src/test/java/.../api/scoring/ProductScoringOrchestratorTest.java`，兩案例：①六項因子皆無資料時 `skippedInsufficientData` 計數正確且 `ProductScoreRepositoryPort#save` 不被呼叫（§5.7）；②兩品項同品類、`MARGIN` 原始值不同（0.10／0.30）時，正規化後的百分位為 25.00／75.00——直接證明母體是整批合併而非逐品項各自為政（§5.10 兩段式順序的核心正確性）。未做端到端比對 §11.1 黃金案例：orchestrator 走的是全新的資料來源鏈（review/sales/audience/festival/climate/heat 六條），要湊出黃金案例的精確輸入需要的 mock 設置量遠超單元測試的效益，且黃金案例本身已在 `ScoringEngineGoldenCaseTest`（`ssds-core`）覆蓋了計算邏輯本身。`./gradlew :ssds-api:test :ssds-core:test` 全綠（含既有 `SsdsApplicationTests` 全 context 啟動測試）。
- **未做，留給下一步**：①§5.10 觸發時機表（排程 cron、API 手動觸發、資料匯入完成後重算等）完全沒接，目前只有 `runFullBatch` 這個方法可以被呼叫，也還沒有人在本機 DB 上實際跑過一次完整批次驗證端到端；②單筆評分（品項新增/編輯、人工覆寫情境）尚未實作，理論上應該直接用 `CategoryPercentilePopulationPort` 查既有母體，不必像全量批次一樣在記憶體組母體；③三個扣分規則的 `risk_rule.threshold_json` key 名稱與初始點數仍是本次定義的假設值（規格書標為附錄 A 待客戶確認項），2026-08-28 已補齊本機 seed（見下方「`risk_rule` 資料對齊 v3.0」），但數值本身仍待 SYS_ADMIN／客戶正式確認，非定案。

**2026-08-27 事後審視修正**：對照 開發規格書_v3.0.md 重新檢查上述實作，發現 `CVR`（§5.2.3）與 `CLIMATE` 地區碼（§7.2／附錄 B）兩處不是規格未定義，而是實作當下沒查全規格、憑空做了假設——規格其實已經寫死答案。已改為照規格實作（見上方 `CVR`／`CLIMATE` 條目），並補上連帶發現的 `PER_IMPUTED_FACTOR` 信心度扣分缺口。`SalesRecordRepository` 移除舊的 `findConversionRate`（時間窗版），新增 `findOwnConversionRate`／`findConversionRatiosByCategoryId`／`sumQtyByProductInCategoryAndDateRange` 三支查詢。`ProductScoringOrchestratorTest` 同步更新 mock。`./gradlew :ssds-api:test :ssds-core:test` 全綠。

**§5.10 兩段式批次順序的 schema 衝突（2026-08-28，補進規格書）**：規格書字面要求第一段把原始值寫進 `score_factor`，但 `score_factor.score_id` 是 `NOT NULL REFERENCES product_score(id)`，而 `product_score.grade` 也 `NOT NULL`——沒有已存在的 `product_score` 列就掛不了 `score_factor`，但 `product_score` 列必須等分數算完才能產生，字面順序做不到。已在 開發規格書_v3.0.md §5.10 補一段 v3.0 附註，記錄 orchestrator 目前的實作（記憶體版兩段式、最終才各寫一次 DB）與正確性理由，並說明真要落 DB 需要的 schema 變更方向（`score_factor` 脫離 `product_score` 外鍵）。純文件修正，未動程式或 DB。

## `risk_rule` 資料對齊 v3.0（2026-08-28）

本機 DB（遠端 pg_dump 鏡像）原本就帶著 `REVIEW_RISK`／`LOGISTICS_RISK`／`INVENTORY_RISK` 三筆全域 `risk_rule` 列，但格式是 v2.0 舊模型：camelCase key（如 `negativeRateThreshold`）、`LOGISTICS_RISK` 用 `{"conditions":[...]}`（命中任一條件即扣滿 `max_penalty`，不是逐條件加總）、`max_penalty` 為 12／13／15，跟 §5.2.2 v3.0 定義的逐條件點數表、`FactorCode` 的結構性上限（10／10／20）都對不上。§5.2.2 原文本身就說明這正是「v2.0 只給上限與觸發條件文字描述」的樣子——按 CLAUDE.md「spec 與現況衝突時 spec 贏」，改用 `V22__align_risk_rule_seed_to_spec_v3.sql`（**UPDATE 而非 INSERT**，避免撞 `uk_risk_rule` 唯一鍵）把這三筆全域列的 `threshold_json`／`max_penalty` 對齊 orchestrator 目前讀取的 key 名稱與 §5.2 的結構性上限。`category_id = 12` 的 `INVENTORY_RISK` 品類覆寫列**刻意未動**，是否沿用 v2.0 門檻待 SYS_ADMIN 確認。**只套用到本機 Docker Postgres**（`.env` 全程指向 `localhost`，未切換到遠端連線設定），未觸碰共用 Supabase。`./gradlew :ssds-api:test :ssds-core:test` 全綠（含本機 DB 跑過 Flyway V22 的 `SsdsApplicationTests`）。

## FR-03 品項管理（前後端全端，2026-08-28）

依規劃「哪些功能可以前後端一起做」清單完成的第一項：`ssds-infra` 的 `Product` 相關 schema/entity 早就備妥、`ssds-api` 只有 `TrendController`／`AuthController`，這條線可以獨立於 AI（Track 3）與評分批次排程（Phase 2 觸發時機表）之外先做完整 CRUD。

**後端**
- 新增 `ssds-api/product/`（`ProductService`、`ProductSpecifications`）與 `controller/ProductController`／`CategoryController`／`SupplierController`，對應 §8.2 `/products`、`/categories`、`/suppliers`。實作範圍：`GET /products`（篩選：`keyword`／`categoryId`／`supplierId`／`trackType`／`sourcingStatus`／`status`，分頁排序）、`GET /products/{id}`、`POST`、`PUT`、`PATCH /{id}/status`、`DELETE`（軟刪除）。
  - **`grade`／`minScore`／`maxScore`／`hasRisk` 四個查詢參數目前不受理**（傳了會被忽略，不回錯誤）：這四個需要 join `product_score`／`risk_alert`，屬於評分批次接上後才能做的範圍，原因與下面 FR-04 的說明相同——見 `ProductSpecifications` 的 class Javadoc。
  - `ProductController.changeStatus` 只允許 §7.4 狀態機裡「不經決策」的兩條直接轉換（`DRAFT→EVALUATING`、`ADOPTED→LISTED`）；其餘轉換（`WATCHING`／`REJECTED`／`REJECTED→EVALUATING`）綁在「建立決策」（FR-11，尚未實作），目前一律回 `409 INVALID_STATE_TRANSITION`。
  - 權限依 §2.1：清單/詳情含 `VIEWER`（第 2 列），新增/編輯/改狀態排除 `VIEWER`（第 4 列），軟刪除僅 `BUYER_LEAD`／`SYS_ADMIN`（第 5 列）。
- **`Product` entity 補上規格已定義但先前漏掉的欄位**：`deletedAt`／`deletedBy`（§7.2、§7.4 軟刪除，DB 早在 V17 就有這兩欄，entity 一直沒對應）、`idealTempMin`／`idealTempMax`（§FR-03-2「供評分使用的欄位」、CLIMATE 因子輸入，DB 在 V13 就有）。加了 `@SQLRestriction("deleted_at IS NULL")`，所有既有查詢（含 `ProductRepository` 既有方法）自動排除軟刪除列，不用逐一改查詢條件。
- `ProductScoreRepository` 新增 `findActivePrimaryByProductIds`，品項清單的「最新分數」欄位一次批次查完，避免逐列 N+1。
- **openapi-generator 的 operationId 衝突**：`CategoryController`／`SupplierController`／`TrendController` 若都用預設方法名 `list`，openapi-generator 產生 TS client 時會把後兩者自動改名成 `list1`／`list2` 這種脆弱名稱（每次重新排序都可能變號）。已把後端方法名固定成 `listCategories`／`listSuppliers`，往後新加清單端點務必用有語意的方法名，不要用裸 `list`。

**前端**
- `product-list`／`product-detail`／`product-form` 三個 stub 補齊實作，串接 `ProductControllerService`（openapi-generator 產出）。清單頁含篩選、分頁（20/50/100）、依 B 軌規則隱藏成本/售價/毛利率/分數/分級欄位（§FR-03-1）。
- 新增共用元件 `shared/components/grade-chip`（原本也是空 stub），品項清單與詳情共用分級徽章。`score-bar`／`empty-state`／`page-header` 仍是空 stub，本次未用到、未補。
- 路由新增 `/products/new`、`/products/:id`、`/products/:id/edit`（原本 `app.routes.ts` 只有 `/products` 清單）。
- `openapi.json` 已重新從本機後端 `/v3/api-docs` 匯出並重新 `npm run generate:api`，`core/api` 新增 `productController`／`categoryController`／`supplierController` 的 service + model。**`authController.service.ts` 也一併生成了**（先前 `AuthController` 存在但沒人重新匯出過 openapi.json）；現有的 `core/auth/auth.service.ts` 手刻版本繼續使用，兩者並存，未合併，如日後要統一改走 generated client 需另外處理。

**驗證**
- 後端：對本機 DB 手動跑過 login／list／get／篩選／create（含成本≥售價擋存、同名警告）／狀態機（合法轉換成功、非法轉換 409）／軟刪除（非 owner role 403、SYS_ADMIN 200 後 404）全部路徑，`./gradlew build -x test` 與 `:ssds-api:test :ssds-core:test` 全綠。
- 前端：`npx ng build` 過（僅 bundle size 略超預算的警告，非錯誤）；`npx ng test --watch=false` 42 個檔案中 3 個既有失敗（`app.spec.ts`、`main-layout`、`confirm-dialog`，皆為既有 baseline 失敗，與本次改動無關，已用 `git stash` 比對確認）之外全綠，含新增的 3 個 spec。
- **本機種子帳密**（已知問題 1 的部分繞過）：把 `buyer@ssds.dev`／`sysadmin@ssds.dev` 兩個本機帳號的 `password_hash` 改成明碼 `Test@12345`（BCrypt），方便本機開發登入測試。**只動了本機 Docker Postgres**，未觸碰共用 Supabase；known issue 1（V900 seed 密碼對不上明碼）本身仍未修。

**未做，留給下一步**
1. `POST /products/{id}/images`、`PUT /products/{id}/festival-affinity`、`POST /products/batch/analyze`（§8.2 品項相關端點，但依賴圖片儲存／節慶維護／AI 任務，超出本次範圍）。
2. B 軌新增流程的關鍵字選擇 UI（目前表單只提示「請至少關聯一個關鍵字」，未做關鍵字下拉/搜尋元件）。
3. 品項清單依分數/分級/風險篩選（見上方「不受理」說明）。

## FR-10 風險示警中心（前後端全端，2026-08-28）

規劃清單第二項。`risk_alert` 的 entity/repo 早就存在（`ProductScoringOrchestrator` 已會寫 `PENALTY_CAP`／`DATA_INSUFFICIENT` 兩種示警），但沒有任何 controller 曝露出來，且本機 DB 種子資料裡已經有 `REVIEW_RISK`／`LOGISTICS_RISK`／`INVENTORY_RISK`／`PENALTY_CAP`／`HEAT_CRASH`／`SEASON_MISMATCH`／`LOW_CONFIDENCE` 七種示警的真實資料可以直接測，同樣不受 Track 3（AI）／評分批次排程未接上的影響。

**後端**
- 新增 `ssds-api/risk/`（`RiskAlertService`、`RiskAlertSpecifications`）與 `controller/RiskAlertController`，對應 §8.2 `GET /risks`（篩選 `status`／`severity`／`type`／`categoryId`，未指定 `status` 時依 AC-10-2 預設排除 `IGNORED`）、`PATCH /risks/{id}/acknowledge`、`PATCH /risks/{id}/ignore`（理由必填，對齊 DB 既有的 `ck_risk_ignore_reason` CHECK）。
- `RiskAlertRepository` 加上 `JpaSpecificationExecutor`，篩選組合邏輯與 FR-03 的 `ProductSpecifications` 同一套寫法。
- 權限依 §2.1：清單含 `VIEWER`（第 2 列），標記已處理／忽略排除 `VIEWER`／`DATA_ADMIN`（第 14 列：`BUYER`／`BUYER_LEAD`／`SYS_ADMIN`）。
- **`GET /risks/rules`／`PUT /risks/rules/{code}`（§FR-10-3 風險門檻調整＋觸發背景全量扣分重算）本次未做**：門檻讀取本身簡單（`RiskRuleRepositoryPort` 已存在），但規格要求「立即觸發背景全量扣分重算，頁首顯示進度」，需要非同步任務追蹤機制，目前 `ProductScoringOrchestrator.runFullBatch` 只是個同步方法、沒有任何任務佇列/進度回報基礎設施，屬於下一步「評分批次觸發時機表」要一併解決的範圍，不在本次 MVP 內單獨湊一個簡化版。
- `RiskAlertController.listRisks`（不叫裸 `list`，理由同 FR-03 筆記：避免 openapi-generator 跨 controller 撞名產生 `list1`/`list2`）。

**前端**
- `features/risks/risks.component.*` 從空 stub 補齊：篩選（狀態、嚴重度）、分頁、標記已處理／忽略（忽略跳自訂的 `ignore-reason-dialog.component.ts` 收必填理由，未套用既有的 `DialogService.Confirm`，因為那支只支援是非確認、不支援文字輸入）、點品項名稱跳轉 `/products/:id`。
- `openapi.json` 已重新匯出、`npm run generate:api` 已重跑，`core/api` 新增 `riskAlertController` service/model。

**驗證**
- 後端：對本機 DB 手動跑過清單（預設排除 IGNORED／篩 `status=IGNORED`）、`acknowledge`、`ignore`（缺理由 400、附理由 200）、角色門檻（`VIEWER` 查清單 200、改狀態 403）；測試用的 2 筆狀態異動已還原回 `OPEN`。`:ssds-api:test :ssds-core:test` 全綠。
- 前端：`npx ng build` 過；`npx ng test --watch=false` 同 FR-03 的 3 個既有 baseline 失敗之外全綠，含新增的 risks spec。

## FR-08 情境權重組設定（前後端全端，2026-08-28）

規劃清單第三項。`weight_version`／`weight_profile`／`grade_threshold` 三張表的 entity/repo 早就存在（評分引擎讀取用），本機 DB 也有真實的三個版本（v1 RETIRED、v2 APPROVED 且生效中、v3 DRAFT 校準建議）可以直接測。

**後端**
- 新增 `ssds-api/weight/WeightVersionService` 與 `controller/WeightVersionController`，對應 §8.2 `GET /weight-versions`、`GET /weight-versions/active`、`GET /weight-versions/{id}/profiles`、`POST /weight-versions`（建立草稿）、`PUT /weight-versions/{id}`（編輯草稿）。
- 權限依 §2.1：讀取含 `VIEWER`（第 2 列）；建立/編輯草稿**僅 `BUYER_LEAD`**（第 15 列——矩陣上這一列連 `SYS_ADMIN` 都沒有，故意的，見規格書「拆分調整評分權重」修正）。
- **`POST /weight-versions/{id}/approve`（§FR-08-3 核准生效並觸發全量重算）本次未做**：規格要求核准後立即觸發背景全量扣分重算並在頁首顯示進度，但目前 `ProductScoringOrchestrator.runFullBatch` 只是個同步方法，沒有任務佇列/進度回報機制——跟 FR-10 的 `PUT /risks/rules/{code}` 是同一個缺口，都排進「評分批次觸發時機表」再一併處理，不湊簡化版。
- AC-08-1 驗證（同一情境六因子權重加總須為 1.000，且不可混入扣分因子）在 `WeightVersionService.validateScenes` 做，命中回既有的 `WEIGHT_SUM_INVALID`（409）。
- **踩到一個雷**：`applyScenes` 存完 `WeightProfile` 後，若直接 `weightVersionRepository.findWithProfilesById(id)` 重新查詢，因為 JPA 一級快取命中同一個受管理的 `WeightVersion` 實例，`profiles` 集合不會真的重新載入，回傳的權重會是空的——已改成存檔當下同步把新列加進 `version.getProfiles()`，不能只依賴重新查詢。若之後有類似「存完子表再讀父實體聚合欄位」的寫法，記得這個陷阱。
- `WeightVersionController` 的 `list`／`create`／`update` 這次直接命名為 `listWeightVersions`／`createWeightVersion`／`updateWeightVersion`（連同本次順手把 `ProductController` 的 `list`／`create`／`update` 也改名為 `listProducts`／`createProduct`／`updateProduct`）：openapi-generator 對裸 `list`/`create`/`update` 這類菜市場名字跨 controller 撞名時的解法（自動加 `1`/`2` 尾碼）不穩定，同一批端點重新排序就可能換編號，前端程式碼看起來對不上意圖。**往後任何新 controller 的方法名都不要用裸的 `list`／`get`／`create`／`update`／`delete`，一律加資源名字首碼。**

**前端**
- `features/weights/weights.component.*` 從空 stub 補齊：版本清單 + 點列檢視四情境權重與門檻明細；`BUYER_LEAD`（`AuthService.hasRole('BUYER_LEAD')`，未套用空殼的 `has-role.directive.ts`）可「新增草稿版本」（預填目前檢視中版本的權重當起點，沒有就給全 0）與「編輯」草稿，逐因子輸入框即時顯示加總是否為 1.000。
- 忽略示警的自訂 dialog（`ignore-reason-dialog.component.ts`）與本次共用同一批 Angular Material 表單元件慣例。

**驗證**
- 後端：對本機 DB 手動跑過清單、`active`、`profiles`（核對真實 v2 的四情境權重與門檻）、`BUYER` 建立草稿 403、`BUYER_LEAD` 建立草稿（權重總和錯誤 409 → 修正後 200）、編輯草稿 200、編輯已核准版本 409；測試建立的草稿已刪除還原。`:ssds-api:test :ssds-core:test` 全綠。
- 前端：`npx ng build` 過；`npx ng test --watch=false` 同前兩項 FR 的 3 個既有 baseline 失敗之外全綠，含新增的 weights spec。

## 規格書／畫面示意圖對照發現的缺口，已補（2026-08-28）

Review FR-03／FR-10／FR-08 三項時，對照 開發規格書_v3.0.md 與 畫面功能示意圖_v3.0.html 發現兩處畫面已經在用、但規格書 §8.2 從未定義的端點，已補進規格書（純文件修正，未動 DB）：

1. **`/products/batch/*` 三個批次端點**（`POST .../queue-score`、`PATCH .../category`、`PATCH .../status`）：§4 FR-03-1 表格與 S-03 畫面都有這三個批次操作，但 §8.2 先前只有 `POST /products/batch/analyze`（批次建立 AI 任務，完全不同的動作），三個操作本身連端點路徑都沒定義過。
2. **`GET /weight-versions/{id}/scene-stats`**：S-09 畫面的「AI 選組規則卡」顯示判定數／覆寫率，資料來源是 `scene_classification_log`，但 §8.2 weight-versions 端點表與 AC-08-1～6 都沒提過這支端點。

兩處都是「畫面已經在用、規格書漏定義」，不是畫面畫錯，所以修的是規格書 §8.2，畫面示意圖不用動。**這三個批次端點與 scene-stats 端點本身尚未實作**（FR-03 本次只做了 GET/POST/PUT/PATCH status/DELETE，見上方「FR-03 品項管理」段落的「未做，留給下一步」），此次只補文件缺口，實作留待之後。

## 本次順手修的程式碼問題：API 回應時間格式不合 §8.1

FR-03／FR-10／FR-08 三個 DTO（`ProductDetail`、`RiskAlertListItem`、`WeightVersionSummary`）的時間欄位原本直接沿用 entity 的 `Instant`（UTC），序列化到前端會帶 `Z` 尾碼，違反 §8.1「回應一律以 +08:00 呈現」（`CLAUDE.md` 也明文禁止 API 層用 `Instant`）。新增 `ApiTime.from(Instant)`（`ssds-api/common/util/`）統一轉換為 `Asia/Taipei` 的 `OffsetDateTime`，三個 Service 的 mapping 方法都改用這支，不在各處各自 `atZone(...)`。已對本機 DB 驗證三個端點回應皆為 `+08:00`，`:ssds-api:test :ssds-core:test` 全綠，前端已重新 `generate:api` 並 `ng build` 過。**往後任何新 DTO 若要回傳實體的時間欄位，一律經過 `ApiTime.from(...)`，不要直接把 `Instant` 放進 record。**

## 前後端全端功能規劃：後續順序

FR-03／FR-10／FR-08 三項（皆不依賴 Track 3 AI 或評分批次排程）已完成。共同的技術債／下一步：
1. **評分批次觸發時機表**（§5.10）：FR-10 的門檻調整重算、FR-08 的核准重算，都卡在這裡沒有非同步任務追蹤機制。這是目前唯一同時擋住兩個功能繼續往下做（門檻調整、版本核准）的關鍵路徑，優先度最高。
2. FR-04 選品分數排行：需要先有第 1 點的排行資料才有東西可查，其餘（`GET /scores/ranking` 等唯讀端點）依賴不大，可以先做唯讀清單，示範資料已有 `product_score` 種子列可用。
3. FR-05 品項詳情的 AI 洞察區塊、FR-07 AI 任務中心：依賴 Track 3（AI），未動工前無法測。

## 平行開發注意事項

1. Phase 0 的 DB schema / port 介面 / OpenAPI 契約沒定案前，別開 Phase 1 的 worktree — 各 track 對介面理解會分歧，事後合併對不上。
2. `ssds-calibration` 依賴 `ssds-ai` 的 AI 解讀部分（§FR-15），該部分建議排在 Track 3 先出介面（interface/DTO），Track 6 對 interface 開發，不等 Track 3 全部完工。
3. 評分引擎（Track 1）務必先寫 §11.1 黃金案例測試，其他 track 平行進行時，Track 1 的正確性可獨立驗證，不受其他 track 進度影響。
4. 次要項目（標 O）時程不夠時依 §12.1 優先序捨棄，捨棄方式已在規格書列出降級方案，不要自行發明新降級規則。
