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

**已知問題（尚未修，使用者已表示資料庫與依賴先不動）**
1. **`V900__seed_master_data.sql` 的種子密碼對不上**：註解宣稱明碼是 `Ssds@2026`，但實測所有帳號（`buyer@ssds.dev` 等）用這組密碼登入皆失敗，雜湊值與該明碼不匹配。目前沒有任何已知明碼能登入種子帳號。修法：重新產生正確的 BCrypt hash，開一支新的 dev migration（如 `V901`）覆蓋，而不是改 V900（已套用過，改了會讓 Flyway checksum 對不上）。
2. **`ssds-infra` 測試編譯失敗**：`MigrationVerificationTest.java` 用了 Testcontainers（`org.testcontainers.*`），但 `ssds-infra/build.gradle` 沒宣告這個測試依賴，`./gradlew build`（不加 `-x test`）會在 `:ssds-infra:compileTestJava` 失敗。這是既有問題，不是 FR-01 改動造成的。跑 `./gradlew build -x test` 或針對個別模組 `:ssds-api:test` / `:ssds-core:test` 可繞開。

下一步照 §12.1／本檔 Phase 1 規劃，建議接著做 `ssds-core/scoring` + `port/`（評分引擎是後續 FR-02/04/05 的關鍵路徑）。

## 平行開發注意事項

1. Phase 0 的 DB schema / port 介面 / OpenAPI 契約沒定案前，別開 Phase 1 的 worktree — 各 track 對介面理解會分歧，事後合併對不上。
2. `ssds-calibration` 依賴 `ssds-ai` 的 AI 解讀部分（§FR-15），該部分建議排在 Track 3 先出介面（interface/DTO），Track 6 對 interface 開發，不等 Track 3 全部完工。
3. 評分引擎（Track 1）務必先寫 §11.1 黃金案例測試，其他 track 平行進行時，Track 1 的正確性可獨立驗證，不受其他 track 進度影響。
4. 次要項目（標 O）時程不夠時依 §12.1 優先序捨棄，捨棄方式已在規格書列出降級方案，不要自行發明新降級規則。
