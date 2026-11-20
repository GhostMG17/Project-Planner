--liquibase formatted sql

--changeset mukhammad:012-create-report-log
CREATE TABLE report_log (
                            id          BIGSERIAL PRIMARY KEY,
                            project_id  BIGINT      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
                            user_id     BIGINT      NOT NULL REFERENCES users (id),
                            created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_report_log_project ON report_log (project_id, created_at);