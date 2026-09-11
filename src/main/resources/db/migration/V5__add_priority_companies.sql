-- =============================================================================
-- V5__add_priority_companies.sql
-- Adds all remaining Priority A / B / C target companies.
-- Source types:
--   GREENHOUSE — public Greenhouse ATS board (board_slug required)
--   LEVER      — public Lever ATS board (board_slug required)
--   LINKEDIN   — LinkedIn Jobs search (no board_slug needed)
-- =============================================================================

-- ── Priority A ────────────────────────────────────────────────────────────────
INSERT INTO companies (name, career_url, active, source_type, board_slug) VALUES
    ('NVIDIA',      'https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite', TRUE, 'LINKEDIN',    NULL),
    ('Databricks',  'https://www.databricks.com/company/careers',                   TRUE, 'GREENHOUSE',  'databricks'),
    ('Stripe',      'https://stripe.com/jobs',                                       TRUE, 'GREENHOUSE',  'stripe'),
    ('Snowflake',   'https://careers.snowflake.com',                                 TRUE, 'GREENHOUSE',  'snowflake'),
    ('Rubrik',      'https://www.rubrik.com/company/careers',                        TRUE, 'GREENHOUSE',  'rubrik'),
    ('LinkedIn',    'https://careers.linkedin.com',                                  TRUE, 'LEVER',       'linkedin');

-- ── Priority B ────────────────────────────────────────────────────────────────
INSERT INTO companies (name, career_url, active, source_type, board_slug) VALUES
    ('Confluent',    'https://www.confluent.io/careers',                   TRUE, 'GREENHOUSE', 'confluent'),
    ('MongoDB',      'https://www.mongodb.com/company/careers',            TRUE, 'GREENHOUSE', 'mongodb'),
    ('ServiceNow',   'https://careers.servicenow.com',                     TRUE, 'GREENHOUSE', 'servicenow'),
    ('Airbnb',       'https://careers.airbnb.com',                         TRUE, 'GREENHOUSE', 'airbnb'),
    ('CrowdStrike',  'https://careers.crowdstrike.com',                    TRUE, 'GREENHOUSE', 'crowdstrike'),
    ('Coinbase',     'https://www.coinbase.com/careers',                   TRUE, 'GREENHOUSE', 'coinbase');

-- ── Priority C ────────────────────────────────────────────────────────────────
INSERT INTO companies (name, career_url, active, source_type, board_slug) VALUES
    ('Broadcom',          'https://careers.broadcom.com',                    TRUE, 'LINKEDIN', NULL),
    ('Cisco',             'https://jobs.cisco.com',                          TRUE, 'GREENHOUSE', 'cisco'),
    ('Intuit',            'https://jobs.intuit.com',                         TRUE, 'GREENHOUSE', 'intuit'),
    ('PayPal',            'https://careers.paypal.com',                      TRUE, 'GREENHOUSE', 'paypal'),
    ('Walmart Global Tech','https://careers.walmart.com',                    TRUE, 'LINKEDIN',   NULL),
    ('SAP',               'https://jobs.sap.com',                            TRUE, 'LINKEDIN',   NULL),
    ('Visa',              'https://www.visa.com/careers',                    TRUE, 'GREENHOUSE', 'visa'),
    ('Mastercard',        'https://careers.mastercard.com',                  TRUE, 'GREENHOUSE', 'mastercard'),
    ('American Express',  'https://www.americanexpress.com/en-us/careers',   TRUE, 'LINKEDIN',   NULL);
