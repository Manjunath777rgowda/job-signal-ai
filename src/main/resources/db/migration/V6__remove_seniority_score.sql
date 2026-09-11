-- =============================================================================
-- V6__remove_seniority_score.sql
-- Drops the seniority_score column from job_matches.
-- The 10% weight is redistributed to domain_score (now 20%).
-- =============================================================================

ALTER TABLE job_matches DROP COLUMN seniority_score;
