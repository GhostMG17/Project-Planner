--liquibase formatted sql

--changeset mukhammad:002-create-projects
CREATE TABLE projects (
                          id                  BIGSERIAL PRIMARY KEY,
                          code                VARCHAR(10)  NOT NULL UNIQUE,
                          name                VARCHAR(200) NOT NULL,
                          client              VARCHAR(200) NOT NULL,
                          description         TEXT,
                          key_deliverables    TEXT,
                          project_manager_id  BIGINT       NOT NULL REFERENCES users (id),
                          status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
                          start_date          DATE         NOT NULL,
                          planned_end_date    DATE         NOT NULL,
                          telegram_chat_id    BIGINT       NOT NULL,
                          telegram_thread_id  INTEGER      NOT NULL,
                          created_by_id       BIGINT       NOT NULL REFERENCES users (id),
                          created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          CONSTRAINT chk_projects_dates CHECK (planned_end_date >= start_date),
                          CONSTRAINT uq_projects_topic UNIQUE (telegram_chat_id, telegram_thread_id)
);

CREATE INDEX idx_projects_pm ON projects (project_manager_id);
CREATE INDEX idx_projects_status ON projects (status);