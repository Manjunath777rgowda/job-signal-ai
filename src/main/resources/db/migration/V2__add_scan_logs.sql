-- =============================================================================
-- V2__add_scan_logs.sql
-- Scan audit log: one row per collection run, one detail row per company.
-- =============================================================================

-- ── scan_logs ─────────────────────────────────────────────────────────────────
CREATE TABLE scan_logs (
    id              BIGSERIAL    PRIMARY KEY,
    started_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ,
    total_fetched   INTEGER      NOT NULL DEFAULT 0,
    total_ingested  INTEGER      NOT NULL DEFAULT 0,
    triggered_by    VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULER',  -- SCHEDULER | MANUAL
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── scan_log_entries ──────────────────────────────────────────────────────────
CREATE TABLE scan_log_entries (
    id          BIGSERIAL   PRIMARY KEY,
    scan_log_id BIGINT      NOT NULL REFERENCES scan_logs(id) ON DELETE CASCADE,
    company     VARCHAR(255) NOT NULL,
    fetched     INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scan_log_entries_scan_log_id ON scan_log_entries(scan_log_id);
