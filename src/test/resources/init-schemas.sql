-- Pre-create the liquibase metadata schema before Liquibase runs.
-- The application schema (lims) is created by changeset 202605041030-create-lims-schema.
CREATE SCHEMA IF NOT EXISTS lims_liquibase;
