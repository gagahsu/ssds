# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

團購產業 AI 採購選品策略系統（SSDS）— decision-support tool that scores candidate products for a group-buying retailer using weighted factors + AI-generated insights. It never auto-orders; it only recommends.

Full functional/data spec: `開發規格書_v3.0.md` (source of truth for behavior, DB schema, API shape). Screen mockups: `畫面功能示意圖_v3.0.html`. Module task breakdown: `docs/module-tasks.md`.

Two independent repos-in-one-checkout, no shared code — contract is `openapi.json` (backend is the source of truth for it):

- `ai-products-selection-backend/` — Gradle multi-module Spring Boot 4.1 / Java 21
- `ai-products-selection-frontend/` — Angular 21 + Angular Material

**Current state (check before assuming a feature exists):** DB schema and `ssds-infra` entities/repositories are complete and aligned to spec. `ssds-core` has only domain enums/DTOs — the scoring engine (`§5` of the spec) and the `port/` interfaces are not yet written. `ssds-ai`, `ssds-ingest`, `ssds-calibration` are empty. `ssds-api` has `TrendController` and, as of FR-01, full JWT auth (`AuthController` + `ssds-api/security/*` — login/refresh/logout/me, lockout, stateless filter chain). Frontend routing skeleton matches the spec's route table but most feature folders are empty stub components; `products` and `admin` have real content. Treat `openapi.json` as possibly stale until the backend controllers it describes actually exist. See `docs/module-tasks.md` for the full progress/known-issues log — check it before starting new module work.

## Commands

### Backend (`ai-products-selection-backend/`)

```bash
./gradlew build                          # build all modules
./gradlew :ssds-core:test                # test a single module
./gradlew test --tests "*ClassName*"     # run a single test class
./gradlew :ssds-api:bootRun              # run the API (needs .env, see below)
```

**This checkout runs against a local Docker Postgres, not the shared Supabase instance.** `ai-products-selection-backend/docker-compose.yml` defines it (`postgres:17-alpine`, matches remote's 17.6); `.env` already points at it (`localhost:5432`, `SPRING_PROFILES_ACTIVE=local`, `SSDS_FLYWAY_ENABLED=true` with local `postgres` superuser creds). Start it with `docker compose up -d` from `ai-products-selection-backend/`. The local DB was seeded on 2026-08-27 via `pg_dump --data-only` from the shared Supabase instance (schema applied fresh from `db/migration` via a throwaway Flyway container, then real data restored) — it is a point-in-time mirror, not live-synced. Do **not** boot with the default `dev` profile against this DB: `dev` additionally applies `db/dev`'s seed migrations (`V900+`), which collide on primary key with the already-restored real data (confirmed by reproducing the failure) — that's why `.env` pins `SPRING_PROFILES_ACTIVE=local`.

Original remote connection details are preserved in `ai-products-selection-backend/.scratch/env.remote.backup` (gitignored) if you need to point back at the shared Supabase instance — copy it over `.env` to switch back. When doing so: don't connect as `postgres`; the app role is `ssds_app` and cannot run DDL or touch `flyway_schema_history`. Only the person applying a migration sets `SSDS_FLYWAY_ENABLED=true` with the `postgres` migration credentials — everyone else leaves Flyway disabled.

### Frontend (`ai-products-selection-frontend/`)

```bash
npm start                 # ng serve, http://localhost:4200
npm run build             # ng build
npm test                  # ng test (Vitest)
npm run generate:api      # regenerate src/app/core/api from ../ai-products-selection-backend's openapi.json
```

## Architecture

### Backend module graph (one-directional, no cycles)

```
ssds-api  →  { ssds-infra, ssds-ai, ssds-ingest, ssds-calibration }  →  ssds-core
```

`ssds-core` has zero dependencies — pure domain model, `ScoringEngine`, `RiskRuleEngine`, and (once written) `port/` repository interfaces that `ssds-infra` implements (dependency inversion). `ssds-ai`, `ssds-ingest`, `ssds-calibration` reach the DB only through those `core` ports, never directly. `ssds-api` is the only module allowed to depend on everything; it wires the Spring context.

Package convention mirrors module boundaries 1:1: `ssds-core` → `com.example.ssds.core`, `ssds-infra` → `com.example.ssds.infra`, etc. This is deliberate — it makes the dependency direction visible in every import line. Don't split a package across modules.

### Scoring model (`§5` of the spec — lives in `ssds-core`, mostly unimplemented)

- Score = bonus subtotal − penalty subtotal. Bonus subtotal is a **single** weighted sum of 6 factors (`TREND`, `CVR`, plus 4 more) against one of four named weight profiles (`SceneType`: VIRAL / FESTIVAL / REPLENISHMENT / SEASONAL) — there is deliberately **no second conversion pass** after the weighted sum (a v2.0 spec bug, fixed in v3.0). Penalty subtotal is 3 fixed risk deductions, unrelated to weights, stored as a **positive** DB value (UI negates it for display).
- A weight *version* (`weight_version`) is an immutable snapshot of all 4 scene profiles + 4 grade thresholds at once; a weight *profile* row is version × scene × single-factor granularity — assembling "one profile" means aggregating multiple rows.
- Any change to scoring logic must keep passing the golden-case regression tests defined in spec `§11.1` before merging.

### Shared conventions that must not drift per-module (`ai-products-selection-backend/CONTEXT.md` has the full rationale — read it before writing a new Controller)

- `@RequestMapping` on controllers is resource-path only (`/products`), never prefixed with `/api/v1` — that prefix is applied globally via `server.servlet.context-path`.
- Date-time fields: `OffsetDateTime` only. Never `Instant` (serializes with `Z`, spec requires `+08:00`) or `LocalDateTime` (no offset). Plain dates: `LocalDate`.
- Pagination: controllers accept `Pageable` directly (never hand-rolled `page`/`size` params), and return `ApiResponse.success(PageResponse.from(page))`, never a raw Spring `Page`.
- Response envelope classes are fixed and singular: `ApiResponse`, `BusinessException`, `ErrorCode`, one project-wide `@RestControllerAdvice`. Don't introduce alternate names for these.
- Enums are stored as `VARCHAR + CHECK`, not native Postgres `ENUM` (a deliberate, documented deviation — Postgres enum alteration is transaction-unsafe).

### Spec vs. implementation precedence

Per `CONTEXT.md`: when the spec and the current DB schema disagree, the **spec wins** — open a migration to fix the schema, don't quietly follow whatever the DB currently does. The two standing, intentional exceptions are the `ENUM`→`VARCHAR+CHECK` translation above and the DB being Postgres/Supabase rather than the MySQL named in spec `§3.2`. Fields the spec's table doesn't list but the implementation already has are kept as intentional extensions; only drop a field the spec explicitly marks removed/deprecated.

### Frontend structure

`src/app/features/<name>/` one folder per screen, matching spec `§9.2`'s route table 1:1 (`app.routes.ts`). `src/app/core/api/` is fully generated (`npm run generate:api`) — never hand-edit it. `src/app/shared/components/` holds cross-feature presentational pieces (`grade-chip`, `score-bar`, `risk-dot`, `ai-panel`, etc.) that encode the spec's visual vocabulary for scores/grades/AI-sourced content — reuse them instead of re-deriving the same badge styling per feature.
