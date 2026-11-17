--liquibase formatted sql

--changeset mukhammad:008-create-notifications
CREATE TABLE notifications (
                               id            BIGSERIAL PRIMARY KEY,
                               trigger_type  VARCHAR(32)  NOT NULL,
                               channel       VARCHAR(16)  NOT NULL,
                               user_id       BIGINT       REFERENCES users (id),
                               task_id       BIGINT       REFERENCES tasks (id) ON DELETE CASCADE,
                               project_id    BIGINT       REFERENCES projects (id) ON DELETE CASCADE,
                               dedup_key     VARCHAR(200) NOT NULL UNIQUE,
                               status        VARCHAR(16)  NOT NULL,
                               attempts      INTEGER      NOT NULL DEFAULT 0,
                               created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               sent_at       TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user ON notifications (user_id);