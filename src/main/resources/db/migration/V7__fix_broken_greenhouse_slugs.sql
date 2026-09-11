-- =============================================================================
-- V7__fix_broken_greenhouse_slugs.sql
-- Switches companies whose Greenhouse board slugs returned HTTP 404 to the
-- LinkedIn collector as a reliable fallback.
-- =============================================================================

UPDATE companies SET source_type = 'LINKEDIN', board_slug = NULL
WHERE name IN (
    'PayPal',
    'Intuit',
    'Cisco',
    'Confluent',
    'Visa',
    'ServiceNow',
    'Mastercard',
    'Snowflake',
    'CrowdStrike'
);
