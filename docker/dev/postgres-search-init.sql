CREATE USER musit PASSWORD 'musit';
grant all privileges on database postgres to musit;


CREATE SCHEMA MUSARK_THING AUTHORIZATION musit;
CREATE SCHEMA MUSIT_PERSON;

-- ===========================================================================
-- Temporary mapping table for museum object's coordinate.
-- ===========================================================================

CREATE TABLE MUSARK_THING.MUSITTHING_SEARCH
(
  objectuuid            UUID not null,
  museumno              TEXT,
  subno                 TEXT,
  term                  TEXT,
  mainobject_id         BIGINT,
  new_collection_id     INTEGER,
  ark_form              TEXT,
  ark_funn_nr           TEXT,
  nat_stage             TEXT,
  nat_gender            TEXT,
  nat_legdato           TEXT,
  is_deleted            BOOLEAN default false,
  aggregated_class_data TEXT,
  updated_date          TIMESTAMP(6) not null,
  museumno_prefix       TEXT,
  museumno_number       INTEGER,
  subno_number          INTEGER,
  document_json         TEXT,
  museumid              INTEGER,
   PRIMARY KEY (objectuuid)
);

CREATE TABLE MUSIT_PERSON.PERSON_NAME(
person_name_uuid UUID NOT NULL,
first_name TEXT,
last_name TEXT,
title TEXT,
name TEXT,
display_name TEXT,
PRIMARY KEY (person_name_uuid)
);


CREATE TABLE MUSIT_PERSON.PERSON(
person_uuid UUID NOT NULL,
legal_entity_type NOT NULL,
date_birth date,
date_dead date,
display_name_uuid UUID
PRIMARY KEY (person_uuid),
FOREIGN KEY FK_PERSONDISPLAYNAME REFERENCES MUSIT_PERSON.PERSON_NAME (person_name_uuid)
);


