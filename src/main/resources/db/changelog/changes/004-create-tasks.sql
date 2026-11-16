--liquibase formatted sql

--changeset mukhammad:004-create-tasks
CREATE SEQUENCE task_number_seq START 1;

CREATE TABLE tasks (
                       id             BIGSERIAL PRIMARY KEY,
                       number         BIGINT       NOT NULL UNIQUE DEFAULT nextval('task_number_seq'),
                       project_id     BIGINT       NOT NULL REFERENCES projects (id),
                       title          VARCHAR(300) NOT NULL,
                       description    TEXT,
                       deliverable    VARCHAR(500),
                       assignee_id    BIGINT       NOT NULL REFERENCES users (id),
                       priority       VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
                       status         VARCHAR(32)  NOT NULL DEFAULT 'NEW',
                       progress       INTEGER      NOT NULL DEFAULT 0,
                       start_date     DATE         NOT NULL,
                       deadline       DATE         NOT NULL,
                       completed_at   TIMESTAMPTZ,
                       created_by_id  BIGINT       NOT NULL REFERENCES users (id),
                       created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT chk_tasks_progress CHECK (progress BETWEEN 0 AND 100),
                       CONSTRAINT chk_tasks_dates CHECK (deadline >= start_date)
);

CREATE INDEX idx_tasks_project ON tasks (project_id);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);
CREATE INDEX idx_tasks_status_deadline ON tasks (status, deadline);