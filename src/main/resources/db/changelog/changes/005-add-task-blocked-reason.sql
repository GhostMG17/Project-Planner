--liquibase formatted sql

--changeset mukhammad:005-add-task-blocked-reason
ALTER TABLE tasks ADD COLUMN blocked_reason VARCHAR(500);