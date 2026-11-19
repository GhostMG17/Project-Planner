--liquibase formatted sql

--changeset mukhammad:011-create-milestones
CREATE TABLE milestones (
                            id             BIGSERIAL PRIMARY KEY,
                            project_id     BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
                            name           VARCHAR(200) NOT NULL,
                            due_date       DATE         NOT NULL,
                            status         VARCHAR(16)  NOT NULL DEFAULT 'PLANNED',
                            completed_at   TIMESTAMPTZ,
                            created_by_id  BIGINT       NOT NULL REFERENCES users (id),
                            created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_milestones_project ON milestones (project_id, due_date);