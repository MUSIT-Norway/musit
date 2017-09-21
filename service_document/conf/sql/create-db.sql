-- INIT SQL script for the document management library database

-- #############################################################################
-- ¡IMPORTANT!
-- The below extension MUST be enabled in the postgres DB.
-- This can only be done by a "superuser". Meaning the postgres DB
-- admin group needs to enable it for our databases, since our postgres user
-- doesn't have that level of authorization.
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- #############################################################################
-- Create necessary schema and table
-- -----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS symbiotic_dman;

CREATE TABLE symbiotic_dman.files (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
  file_id         UUID    NOT NULL,
  version         INTEGER NOT NULL DEFAULT 1,
  file_name       TEXT    NOT NULL,
  path            TEXT    NOT NULL,
  is_folder       BOOLEAN NOT NULL,
  accessible_by   JSONB   NOT NULL,
  content_type    TEXT,
  length          NUMERIC,
  owner_id        TEXT,
  owner_type      TEXT,
  created_date    TIMESTAMP WITH TIME ZONE,
  created_by      UUID,
  description     TEXT,
  locked_by       UUID,
  locked_date     TIMESTAMP WITH TIME ZONE,
  custom_metadata JSONB
);

-- #############################################################################
-- Create useful indices for the above table
-- -----------------------------------------------------------------------------
CREATE INDEX files_file_id_index   ON symbiotic_dman.files (file_id);
CREATE INDEX files_file_name_index ON symbiotic_dman.files (file_name);
CREATE INDEX files_path_index      ON symbiotic_dman.files (path);
CREATE INDEX files_is_folder_index ON symbiotic_dman.files (is_folder);
CREATE INDEX files_owner_id_index  ON symbiotic_dman.files (owner_id);