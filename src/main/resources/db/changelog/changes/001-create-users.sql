--liquibase formatted sql

--changeset mukhammad:001-create-users
CREATE TABLE users (
                       id           BIGSERIAL PRIMARY KEY,
                       telegram_id  BIGINT       NOT NULL UNIQUE,
                       username     VARCHAR(64),
                       first_name   VARCHAR(128) NOT NULL,
                       last_name    VARCHAR(128),
                       role         VARCHAR(32)  NOT NULL DEFAULT 'TEAM_MEMBER',
                       active       BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_username ON users (LOWER(username));