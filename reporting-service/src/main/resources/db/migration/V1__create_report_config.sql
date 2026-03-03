-- V1__create_report_config.sql
-- Reporting service metadata schema

CREATE TABLE IF NOT EXISTS report_config (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    client_id   VARCHAR(255) NOT NULL UNIQUE,
    report_type VARCHAR(100) NOT NULL,
    currency    VARCHAR(10)  NOT NULL DEFAULT 'USD',
    timezone    VARCHAR(100) DEFAULT 'UTC',
    alerts_enabled BOOLEAN   NOT NULL DEFAULT false,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_report_config_client_id ON report_config(client_id);

CREATE TABLE IF NOT EXISTS scheduled_reports (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    client_id       VARCHAR(255) NOT NULL,
    report_type     VARCHAR(100) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT true,
    last_run_at     TIMESTAMP WITH TIME ZONE,
    next_run_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_scheduled_reports_client
        FOREIGN KEY (client_id) REFERENCES report_config(client_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_scheduled_reports_client_id ON scheduled_reports(client_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_reports_enabled ON scheduled_reports(enabled);

CREATE TABLE IF NOT EXISTS alert_rules (
    id                   VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    client_id            VARCHAR(255)   NOT NULL,
    rule_type            VARCHAR(100)   NOT NULL,
    threshold_value      NUMERIC(19, 4),
    threshold_currency   VARCHAR(10),
    enabled              BOOLEAN        NOT NULL DEFAULT true,
    notification_channel VARCHAR(100),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_alert_rules_client
        FOREIGN KEY (client_id) REFERENCES report_config(client_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alert_rules_client_id ON alert_rules(client_id);
CREATE INDEX IF NOT EXISTS idx_alert_rules_enabled ON alert_rules(enabled);

COMMENT ON TABLE report_config IS 'Per-client reporting configuration owned by the reporting microservice';
COMMENT ON TABLE scheduled_reports IS 'Scheduled report definitions (cron-based)';
COMMENT ON TABLE alert_rules IS 'Alert threshold rules for banking analytics';
