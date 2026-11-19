--liquibase formatted sql

--changeset mukhammad:010-create-risks
CREATE TABLE risks (
                       id             BIGSERIAL PRIMARY KEY,
                       project_id     BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
                       description    VARCHAR(500) NOT NULL,
                       severity       VARCHAR(16)  NOT NULL,
                       owner_id       BIGINT       NOT NULL REFERENCES users (id),
                       status         VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
                       created_by_id  BIGINT       NOT NULL REFERENCES users (id),
                       created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       closed_at      TIMESTAMPTZ
);

CREATE INDEX idx_risks_project_status ON risks (project_id, status);