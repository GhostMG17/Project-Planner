--liquibase formatted sql

--changeset mukhammad:006-create-task-history
CREATE TABLE task_history (
                              id             BIGSERIAL PRIMARY KEY,
                              task_id        BIGINT       NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
                              changed_by_id  BIGINT       NOT NULL REFERENCES users (id),
                              change_type    VARCHAR(32)  NOT NULL,
                              old_value      VARCHAR(500),
                              new_value      VARCHAR(500),
                              comment        VARCHAR(500),
                              changed_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_history_task ON task_history (task_id, changed_at);

--changeset mukhammad:006-backfill-task-created
INSERT INTO task_history (task_id, changed_by_id, change_type, new_value, changed_at)
SELECT id, created_by_id, 'CREATED', 'NEW', created_at
FROM tasks;