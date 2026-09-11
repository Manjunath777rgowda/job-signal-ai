-- =============================================================================
-- V3__fix_collector_source_types.sql
-- Switches companies whose native ATS APIs are broken to the LinkedIn collector.
-- Amazon and JPMorgan keep their own working collectors.
-- =============================================================================

UPDATE companies SET source_type = 'LINKEDIN'
WHERE name IN ('Google', 'Microsoft', 'Oracle', 'Meta', 'Apple', 'Adobe', 'Salesforce', 'Uber', 'Atlassian');
