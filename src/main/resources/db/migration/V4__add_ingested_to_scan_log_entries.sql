-- =============================================================================
-- V4__add_ingested_to_scan_log_entries.sql
-- Tracks how many jobs were actually inserted (new) per company per scan,
-- as opposed to the raw fetched count which includes already-seen duplicates.
-- =============================================================================

ALTER TABLE scan_log_entries
    ADD COLUMN ingested INTEGER NOT NULL DEFAULT 0;
