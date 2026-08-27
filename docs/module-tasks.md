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
2. **落實「資料不足」的 schema 變更**：commit 4 只改了規格書文字，**還沒**真的加 `product.last_scoring_status`／`last_scoring_attempted_at` 欄位（需開新 Flyway migration）、也還沒把 `risk_alert.risk_type` 的 CHECK 約束與 `RiskAlert` 相關 enum 加上 `DATA_INSUFFICIENT`。orchestrator 目前受限於這個缺口（見下方說明），建議儘快補上。
3. `LOGISTICS_RISK`／`INVENTORY_RISK` 目前是 code 常數（`LogisticsRiskCalculator`／`InventoryRiskCalculator`），應改為吃 `RiskRuleRepositoryPort` 提供的 `RiskRuleConfig.thresholds()`，不要繼續寫死——現在能過測試是因為測試直接呼叫這兩個類別，接上 orchestrator 後才會露出「SYS_ADMIN 改門檻要重新部署」的問題。
4. 已知問題 1、2（種子密碼、`MigrationVerificationTest` 編譯失敗）仍未修，使用者先前表示資料庫與依賴先不動，維持現狀。
5. Track 3（AI Agent）／Track 4（熱度資料層）／前端可平行推進，不受上述影響，但最終串接評分結果都要經過第 1 點的 orchestrator。

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
- **三個扣分規則**：`LOGISTICS_RISK`／`INVENTORY_RISK` 沿用既有計算器內建的佔位點數（未接 `RiskRuleRepositoryPort`，符合下方已知問題 3 的既定安排，非本次遺漏）。`REVIEW_RISK` 已接 `RiskRuleRepositoryPort.findConfig("REVIEW_RISK", categoryId)`，門檻 key 名稱 **`negative_rate_threshold` 為本次新定義**，資料庫（含 migration）目前查不到任何 `risk_rule` 種子資料，SYS_ADMIN 需補上這筆設定，否則跑批次會直接丟例外（找不到規則）。`risk_topic_share`（負評中屬品質／食安／物流破損的比例）本應由 Agent 2 ReviewRiskAgent（Track 3）分類，Track 3 未實作前改用關鍵字比對 `review_analysis.key_phrase`（`ProductScoringOrchestrator.RISK_TOPIC_KEYWORDS`），屬暫代方案。
- **`ProductReviewRepository` 新增兩支查詢方法**（無 schema 變更）：`countByProductIdAndAnalysisSentiment`、`findKeyPhrasesByProductIdAndAnalysisSentiment`，供上述 `REVIEW_RISK` 使用。
- **`ssds-api/build.gradle` 補了 `testImplementation libs.test.spring.boot`**：webmvc/security/validation 三支 test starter 不保證帶 Mockito，`ssds-core` 也是靠這支測試依賴。
- 測試：`ssds-api/src/test/java/.../api/scoring/ProductScoringOrchestratorTest.java`，兩案例：①六項因子皆無資料時 `skippedInsufficientData` 計數正確且 `ProductScoreRepositoryPort#save` 不被呼叫（§5.7）；②兩品項同品類、`MARGIN` 原始值不同（0.10／0.30）時，正規化後的百分位為 25.00／75.00——直接證明母體是整批合併而非逐品項各自為政（§5.10 兩段式順序的核心正確性）。未做端到端比對 §11.1 黃金案例：orchestrator 走的是全新的資料來源鏈（review/sales/audience/festival/climate/heat 六條），要湊出黃金案例的精確輸入需要的 mock 設置量遠超單元測試的效益，且黃金案例本身已在 `ScoringEngineGoldenCaseTest`（`ssds-core`）覆蓋了計算邏輯本身。`./gradlew :ssds-api:test :ssds-core:test` 全綠（含既有 `SsdsApplicationTests` 全 context 啟動測試）。
- **未做，留給下一步**：①§5.10 觸發時機表（排程 cron、API 手動觸發、資料匯入完成後重算等）完全沒接，目前只有 `runFullBatch` 這個方法可以被呼叫；②單筆評分（品項新增/編輯、人工覆寫情境）尚未實作，理論上應該直接用 `CategoryPercentilePopulationPort` 查既有母體，不必像全量批次一樣在記憶體組母體；③`REVIEW_RISK` 的 `negative_rate_threshold` key 名稱、`risk_topic_share` 關鍵字表，這兩項仍是本次為了讓 orchestrator 能跑而做的假設（`CVR`／`CLIMATE` 已於 2026-08-27 對照規格書修正，不再是假設），建議跟 SYS_ADMIN 確認後視需要調整。

**2026-08-27 事後審視修正**：對照 開發規格書_v3.0.md 重新檢查上述實作，發現 `CVR`（§5.2.3）與 `CLIMATE` 地區碼（§7.2／附錄 B）兩處不是規格未定義，而是實作當下沒查全規格、憑空做了假設——規格其實已經寫死答案。已改為照規格實作（見上方 `CVR`／`CLIMATE` 條目），並補上連帶發現的 `PER_IMPUTED_FACTOR` 信心度扣分缺口。`SalesRecordRepository` 移除舊的 `findConversionRate`（時間窗版），新增 `findOwnConversionRate`／`findConversionRatiosByCategoryId`／`sumQtyByProductInCategoryAndDateRange` 三支查詢。`ProductScoringOrchestratorTest` 同步更新 mock。`./gradlew :ssds-api:test :ssds-core:test` 全綠。

## 平行開發注意事項

1. Phase 0 的 DB schema / port 介面 / OpenAPI 契約沒定案前，別開 Phase 1 的 worktree — 各 track 對介面理解會分歧，事後合併對不上。
2. `ssds-calibration` 依賴 `ssds-ai` 的 AI 解讀部分（§FR-15），該部分建議排在 Track 3 先出介面（interface/DTO），Track 6 對 interface 開發，不等 Track 3 全部完工。
3. 評分引擎（Track 1）務必先寫 §11.1 黃金案例測試，其他 track 平行進行時，Track 1 的正確性可獨立驗證，不受其他 track 進度影響。
4. 次要項目（標 O）時程不夠時依 §12.1 優先序捨棄，捨棄方式已在規格書列出降級方案，不要自行發明新降級規則。
