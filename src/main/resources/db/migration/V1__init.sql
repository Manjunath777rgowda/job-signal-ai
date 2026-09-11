-- =============================================================================
-- V1__init.sql
-- JobSignal AI — complete initial schema with seed data
-- =============================================================================

-- ── companies ─────────────────────────────────────────────────────────────────
CREATE TABLE companies (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    career_url  VARCHAR(512),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    source_type VARCHAR(50),
    board_slug  VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO companies (name, career_url, active, source_type, board_slug) VALUES
    ('Amazon',        'https://www.amazon.jobs',                    TRUE, 'AMAZON',    NULL),
    ('Google',        'https://careers.google.com',                 TRUE, 'GOOGLE',    NULL),
    ('Microsoft',     'https://careers.microsoft.com',              TRUE, 'MICROSOFT', NULL),
    ('JPMorgan Chase','https://careers.jpmorgan.com',               TRUE, 'JPMORGAN',  NULL),
    ('Meta',          'https://www.metacareers.com',                TRUE, 'GREENHOUSE','facebookapp'),
    ('Apple',         'https://jobs.apple.com',                     TRUE, 'LEVER',     'apple'),
    ('Adobe',         'https://careers.adobe.com',                  TRUE, 'GREENHOUSE','adobetech'),
    ('Salesforce',    'https://careers.salesforce.com',             TRUE, 'GREENHOUSE','salesforce'),
    ('Uber',          'https://www.uber.com/us/en/careers',         TRUE, 'GREENHOUSE','uber'),
    ('Atlassian',     'https://www.atlassian.com/company/careers',  TRUE, 'GREENHOUSE','atlassian'),
    ('Oracle',        'https://careers.oracle.com',                 TRUE, 'ORACLE',    NULL);

-- ── jobs ──────────────────────────────────────────────────────────────────────
CREATE TABLE jobs (
    id              BIGSERIAL    PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES companies(id),
    external_job_id VARCHAR(255) NOT NULL,
    title           VARCHAR(512) NOT NULL,
    description     TEXT         NOT NULL,
    location        VARCHAR(255),
    job_url         VARCHAR(1024),
    posted_at       TIMESTAMPTZ,
    first_seen_at   TIMESTAMPTZ,
    last_seen_at    TIMESTAMPTZ,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_company_external_job UNIQUE (company_id, external_job_id)
);

-- ── resume_profiles ───────────────────────────────────────────────────────────
CREATE TABLE resume_profiles (
    id               BIGSERIAL    PRIMARY KEY,
    full_name        VARCHAR(255) NOT NULL,
    headline         VARCHAR(512) NOT NULL,
    summary          TEXT         NOT NULL,
    skills           TEXT         NOT NULL,
    experience       TEXT,
    certifications   TEXT,
    years_experience INTEGER      NOT NULL DEFAULT 0,
    target_roles     TEXT,
    location         VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO resume_profiles (
    full_name, headline, summary, skills, experience,
    certifications, years_experience, target_roles, location
) VALUES (
    'Manjunath R',
    'Senior Staff System Engineer — IBM India Pvt Ltd.',
    'Software Development Engineer and Senior Staff System Engineer with over 8+ years of experience, '
    'specializing in leading teams and designing scalable end-to-end pipelines. Proficient in Microservices, '
    'Database Management, Problem Solving, and Software Designing. Skilled in converting monolithic applications '
    'into modern microservices, ensuring reliability, scalability, and robustness.',
    'Java, Python, Spring Boot, Microservices, REST APIs, Keycloak, Alembic, Docker, Kubernetes, Jenkins, '
    'PostgreSQL, MySQL, MongoDB, Redis, BigQuery, AWS, Azure, GCP, IBM Cloud, Prometheus, Grafana, Graylog, '
    'DSA, OOP, Maven, Linux, Machine Learning, NLP, Computer Vision, SVM, Random Forest, Regression',
    'IBM India Pvt Ltd — Senior Staff System Engineer (May 2024–present): Led backend design of Crypto Discovery '
    'and Inventory application, implemented Keycloak auth, led backend team, managed Alembic DB migrations. '
    'Nuvepro Technologies — Principal Software Engineer (Jan 2021–Apr 2024): Led 5-member team, architected '
    'Nuvelink V2 microservices with Spring Boot, optimized AWS/Azure/GCP cost, migrated Java 8 to 11. '
    'High Peak Software — Software Engineer (Jul 2018–Jan 2021): Built FIU Connect with Spring Boot and MongoDB, '
    'Email Engagement Engine, invoice parser, Campaign Management System.',
    'Postgraduate Degree in AI and ML — Texas McCombs School of Business (2023). '
    'Master Microservices with Spring Boot and Spring Cloud — Udemy (2022). '
    'Master of Data Science (Global) — Deakin University (2024–2025).',
    8,
    'Senior Software Engineer, Staff Software Engineer, Principal Software Engineer, Senior Backend Engineer, Lead Software Engineer',
    'Bengaluru, Karnataka, India'
);

-- ── job_matches ───────────────────────────────────────────────────────────────
CREATE TABLE job_matches (
    id                   BIGSERIAL   PRIMARY KEY,
    job_id               BIGINT      NOT NULL REFERENCES jobs(id),
    resume_profile_id    BIGINT      NOT NULL REFERENCES resume_profiles(id),
    skill_score          INTEGER     NOT NULL DEFAULT 0,
    experience_score     INTEGER     NOT NULL DEFAULT 0,
    responsibility_score INTEGER     NOT NULL DEFAULT 0,
    seniority_score      INTEGER     NOT NULL DEFAULT 0,
    domain_score         INTEGER     NOT NULL DEFAULT 0,
    location_score       INTEGER     NOT NULL DEFAULT 0,
    other_score          INTEGER     NOT NULL DEFAULT 0,
    overall_score        INTEGER     NOT NULL DEFAULT 0,
    interview_fit        VARCHAR(20) NOT NULL,
    recommendation       VARCHAR(20) NOT NULL,
    strengths            TEXT,
    gaps                 TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── applications ──────────────────────────────────────────────────────────────
CREATE TABLE applications (
    id                BIGSERIAL   PRIMARY KEY,
    job_id            BIGINT      NOT NULL REFERENCES jobs(id),
    resume_profile_id BIGINT      NOT NULL REFERENCES resume_profiles(id),
    status            VARCHAR(50) NOT NULL DEFAULT 'SAVED',
    applied_at        TIMESTAMPTZ,
    response_at       TIMESTAMPTZ,
    interview_date    TIMESTAMPTZ,
    notes             TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
