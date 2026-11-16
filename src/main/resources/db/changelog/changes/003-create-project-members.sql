--liquibase formatted sql

--changeset mukhammad:003-create-project-members
CREATE TABLE project_members (
                                 id           BIGSERIAL PRIMARY KEY,
                                 project_id   BIGINT      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
                                 user_id      BIGINT      NOT NULL REFERENCES users (id),
                                 added_by_id  BIGINT      NOT NULL REFERENCES users (id),
                                 added_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 CONSTRAINT uq_project_members UNIQUE (project_id, user_id)
);

CREATE INDEX idx_project_members_user ON project_members (user_id);