--liquibase formatted sql

--changeset mukhammad:009-add-project-risk
ALTER TABLE projects ADD COLUMN at_risk_manual BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE projects ADD COLUMN at_risk_since TIMESTAMPTZ;