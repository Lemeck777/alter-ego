-- Alter Ego core schema.
-- Design note: this database stores CONTENT and ENTITLEMENTS. Personal commitment history stays
-- on the user's device unless they explicitly enable cloud backup.

CREATE TABLE IF NOT EXISTS content_bundles (
    version         BIGINT PRIMARY KEY,
    payload         JSONB       NOT NULL,
    moment_count    INTEGER     NOT NULL,
    claim_count     INTEGER     NOT NULL,
    published_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_by    TEXT        NOT NULL DEFAULT 'system',
    notes           TEXT
);

CREATE INDEX IF NOT EXISTS content_bundles_published_at_idx ON content_bundles (published_at DESC);

-- The evidence database is versioned separately so a corrected claim can be traced.
CREATE TABLE IF NOT EXISTS evidence_claims (
    claim_id            TEXT PRIMARY KEY,
    claim               TEXT        NOT NULL,
    topic               TEXT        NOT NULL,
    age_min             INTEGER,
    age_max             INTEGER,
    evidence_level      TEXT        NOT NULL CHECK (evidence_level IN ('strong','moderate','limited','tradition')),
    direction           TEXT        NOT NULL DEFAULT '',
    source_url          TEXT,
    source_title        TEXT        NOT NULL DEFAULT '',
    publication_year    INTEGER,
    study_type          TEXT        NOT NULL DEFAULT '',
    review_date         DATE        NOT NULL,
    medical_reviewer    TEXT        NOT NULL DEFAULT 'pending',
    review_note         TEXT,
    status              TEXT        NOT NULL DEFAULT 'active' CHECK (status IN ('active','retired')),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS evidence_claim_revisions (
    id              BIGSERIAL PRIMARY KEY,
    claim_id        TEXT        NOT NULL,
    previous        JSONB       NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    changed_by      TEXT        NOT NULL DEFAULT 'system',
    reason          TEXT
);

CREATE INDEX IF NOT EXISTS evidence_claim_revisions_claim_idx ON evidence_claim_revisions (claim_id, changed_at DESC);

-- Accounts exist only for people who choose cloud backup or a subscription.
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    install_id      TEXT UNIQUE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    country         TEXT
);

CREATE TABLE IF NOT EXISTS entitlements (
    user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    is_plus         BOOLEAN     NOT NULL DEFAULT false,
    source          TEXT        NOT NULL DEFAULT 'free',
    product_id      TEXT,
    purchase_token  TEXT,
    expires_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS entitlements_purchase_token_idx ON entitlements (purchase_token) WHERE purchase_token IS NOT NULL;

-- Aggregate product metrics only. No message text, no notes, no quotes.
CREATE TABLE IF NOT EXISTS analytics_events (
    id          BIGSERIAL PRIMARY KEY,
    install_id  TEXT        NOT NULL,
    name        TEXT        NOT NULL,
    props       JSONB       NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS analytics_events_name_idx ON analytics_events (name, occurred_at DESC);
CREATE INDEX IF NOT EXISTS analytics_events_install_idx ON analytics_events (install_id, occurred_at DESC);
