# JobSignal AI

> **Find the jobs where you have the strongest shot.**

A personal AI-powered job intelligence platform. Continuously collects relevant job postings from 32 major MNCs, scores them against a senior engineer's resume profile across 6 dimensions, and surfaces the roles most worth applying to — ranked by interview fit.

**Live at:** `http://localhost:8888`

---

## What it does

1. **Collects** job postings every 6 hours (or on demand) from 32 target companies across Greenhouse, Lever, LinkedIn, Amazon, Google, JPMorgan, Microsoft, and Oracle career APIs
2. **Scores** each job across 6 weighted dimensions — Skills (30%), Experience (20%), Responsibility (20%), Domain (20%), Location (5%), Other (5%)
3. **Recommends** one of: `APPLY NOW` · `APPLY` · `CONSIDER` · `SKIP`
4. **Tracks** status: Active → Applied → Archived, with scan audit logs per run

---

## Dashboard

Single-page dashboard at `/` with three tabs:

| Tab | What it shows |
|-----|---------------|
| **Jobs** | Scored job cards filterable by recommendation, status, and location. Score ring, dimension bars, strengths/gaps tags, action buttons. |
| **Companies** | All 32 companies grouped by priority tier (A/B/C) with match breakdown bar, avg score, applied/archived pills. Click any row to open a side drawer with full job list. |
| **Scan Details** | Audit log of every collection run — start time, duration, jobs fetched/ingested per company. |

Clicking **⟳ Collect Jobs Now** streams live progress via SSE: each company pops up as it finishes.

---

## Tech stack

| Layer | Choice |
|-------|--------|
| Runtime | Java 25 · Spring Boot 3.4 · Tomcat (virtual threads) |
| Persistence | PostgreSQL 17 · Spring Data JPA · Flyway migrations |
| Frontend | Single-file `index.html` — vanilla HTML/CSS/JS, no build tools |
| Scheduling | Spring `@Scheduled` — every 6 hours |
| Streaming | Spring `SseEmitter` — SSE over POST for live collect progress |

---

## Getting started

### Prerequisites

- Java 25+
- PostgreSQL 17 running locally
- Maven 3.9+

### Database setup

```sql
CREATE DATABASE "job-signal-ai";
```

### Configuration

Set environment variables (or create an `.env` file and export them):

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=job-signal-ai
export DB_USERNAME=your_pg_user
export DB_PASSWORD=your_pg_password
```

Or override via `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/job-signal-ai
spring.datasource.username=your_pg_user
spring.datasource.password=your_pg_password
```

### Run

```bash
mvn spring-boot:run
```

Flyway runs all migrations automatically on startup. Open `http://localhost:8888`.

---

## Database migrations

| Version | Description |
|---------|-------------|
| V1 | Schema init + seed (11 founding companies) |
| V2 | `scan_logs` + `scan_log_entries` tables |
| V3 | Fix collector source types (9 companies → LinkedIn) |
| V4 | Add `ingested` column to `scan_log_entries` |
| V5 | Add 21 priority A/B/C companies |
| V6 | Drop `seniority_score` from `job_matches` |
| V7 | Fix 9 broken Greenhouse slugs → LinkedIn |

---

## Target companies (32 total)

**Priority A** — Amazon, Google, Microsoft, Meta, Apple, JPMorgan Chase, Atlassian, Adobe, Salesforce, Uber, Oracle  
**Priority B** — Stripe, Databricks, Snowflake, Confluent, MongoDB, Airbnb, Coinbase, Rubrik, Wiz, CrowdStrike  
**Priority C** — Visa, Mastercard, PayPal, Intuit, ServiceNow, Cisco, LinkedIn, Block, Twilio, Okta, HashiCorp

---

## API reference

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/scan` | All scored jobs (query param `sort=score\|posted`) |
| `POST` | `/api/v1/scan/re-evaluate` | Re-evaluate matches for all jobs (preserves job status) |
| `POST` | `/api/v1/scan/cleanup` | Cleans up all unapplied and unarchived jobs |
| `GET` | `/api/v1/companies` | All companies with priority tiers |
| `POST` | `/api/v1/collect` | Trigger a full collection (fire-and-wait) |
| `POST` | `/api/v1/collect/stream` | Trigger a full collection (SSE streaming) |
| `PATCH` | `/api/v1/jobs/{id}/status` | Update job status (ACTIVE / APPLIED / ARCHIVED) |
| `GET` | `/api/v1/scan/logs` | All scan audit logs (summary, newest first) |
| `GET` | `/api/v1/scan/log/{id}` | Single scan with per-company breakdown |

---

## Matching algorithm

Six dimensions, scored 0–100:

| Dimension | Weight | What it measures |
|-----------|--------|-----------------|
| Skills | 30% | Technical skill overlap (Java, Spring, cloud, etc.) |
| Experience | 20% | Years and seniority alignment |
| Responsibility | 20% | Scope, leadership, cross-functional ownership |
| Domain | 20% | Industry/vertical fit (fintech, platform, infra, etc.) |
| Location | 5% | Remote / hybrid / office preference match |
| Other | 5% | Education, culture signals |

**Recommendations:**

| Score | Recommendation |
|-------|---------------|
| ≥ 75 | `APPLY NOW` |
| ≥ 55 | `APPLY` |
| ≥ 40 | `CONSIDER` |
| < 40 | `SKIP` |

---

## Project layout

```
src/main/java/com/jobsignal/ai/
├── company/          Company entity, repository, controller, SourceType enum
├── ingestion/        Collectors (8 sources), JobCollectorRegistry, JobIngestionService,
│                     CollectController (POST + SSE), JobIngestionScheduler
├── job/              Job entity, repository, JobController (status PATCH), JobStatus enum
├── matching/         MatchingService, ScanController, JobMatch, JobScanResult
├── profile/          ResumeProfile entity + repository
└── scanlog/          ScanLog, ScanLogEntry, ScanLogService, ScanLogController

src/main/resources/
├── static/index.html           Single-page dashboard
├── application.properties      DB config via env vars
├── logback-spring.xml          Rolling log appender
└── db/migration/               Flyway V1–V7 migrations
```

---

## Collectors

| Source type | Companies handled |
|-------------|------------------|
| `GREENHOUSE` | Meta, Atlassian, Adobe, Uber, Salesforce, Databricks, Stripe, Rubrik, MongoDB, Airbnb, Coinbase |
| `LEVER` | Apple, LinkedIn |
| `LINKEDIN` | All 32 companies — fallback + direct |
| `AMAZON` | Amazon |
| `GOOGLE` | Google |
| `MICROSOFT` | Microsoft |
| `JPMORGAN` | JPMorgan Chase |
| `ORACLE` | Oracle |

Collection is parallel — each company runs in its own virtual thread.

---

## Logging

Rolling log at `logs/job-signal-ai.log` — max 10 MB per file, 30 days retention.
