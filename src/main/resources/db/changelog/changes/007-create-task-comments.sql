--liquibase formatted sql

--changeset mukhammad:007-create-task-comments
CREATE TABLE task_comments (
                               id          BIGSERIAL PRIMARY KEY,
                               task_id     BIGINT       NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
                               author_id   BIGINT       NOT NULL REFERENCES users (id),
                               body        TEXT,
                               file_id     VARCHAR(255),
                               file_type   VARCHAR(16),
                               file_name   VARCHAR(255),
                               created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               CONSTRAINT chk_task_comments_content CHECK (body IS NOT NULL OR file_id IS NOT NULL)
);

CREATE INDEX idx_task_comments_task ON task_comments (task_id, created_at);