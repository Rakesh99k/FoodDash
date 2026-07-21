CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id BIGINT,
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_entity_type_entity_id ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_webhook_events_provider_event_id UNIQUE (provider, event_id),
    CONSTRAINT chk_webhook_events_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'FAILED', 'IGNORED'))
);

CREATE INDEX idx_webhook_events_provider ON webhook_events (provider);
CREATE INDEX idx_webhook_events_event_type ON webhook_events (event_type);
CREATE INDEX idx_webhook_events_status ON webhook_events (status);
CREATE INDEX idx_webhook_events_created_at ON webhook_events (created_at);
