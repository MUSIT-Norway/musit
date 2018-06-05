--CREATE USER musit PASSWORD 'musit';
--grant all privileges on database postgres to musit;

--CREATE SCHEMA MUSARK_THING AUTHORIZATION musit;
--CREATE SCHEMA MUSIT_PERSON AUTHORIZATION musit;
--CREATE SCHEMA MUSIT_EVENT AUTHORIZATION musit;

drop schema if exists musark_thing cascade;
drop schema if exists musit_person cascade;
drop schema if exists musit_place cascade;
drop schema if exists musit_event cascade;

CREATE SCHEMA MUSARK_THING;
CREATE SCHEMA MUSIT_PERSON;
CREATE SCHEMA MUSIT_PLACE;
CREATE SCHEMA MUSIT_EVENT;

-- ===========================================================================
-- Temporary mapping table for museum object's coordinate.
-- ===========================================================================

drop table IF EXISTS MUSARK_THING.MUSITTHING_SEARCH;
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
  museumid              INTEGER,
  document_json         TEXT,
   PRIMARY KEY (objectuuid)
);

drop table IF EXISTS MUSARK_THING.MUSITTHING_SEARCH_POPULATING;
CREATE TABLE MUSARK_THING.MUSITTHING_SEARCH_POPULATING
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
  museumid              INTEGER,
  document_json         TEXT,
   PRIMARY KEY (objectuuid)
);

--drop table if exists MUSIT_PERSON.PERSON_NAME;
--CREATE TABLE MUSIT_PERSON.PERSON_NAME(
--person_name_uuid UUID NOT NULL,
--first_name TEXT,
--last_name TEXT,
--title TEXT,
--name TEXT,
--display_name TEXT,
--PRIMARY KEY (person_name_uuid)
--);

drop table if exists MUSIT_EVENT.EVENT_ROLE_PERSON;

drop table if exists MUSIT_EVENT.ROLE;
CREATE TABLE MUSIT_EVENT.ROLE(
role_id SERIAL PRIMARY KEY,
role_text  TEXT NOT NULL,
type_for TEXT NOT NULL
);

drop table if exists MUSIT_PLACE.PLACE;
CREATE TABLE MUSIT_PLACE.PLACE(
place_uuid UUID PRIMARY KEY,
place_name  TEXT NOT NULL
);

drop table if exists MUSIT_EVENT.EVENT_TYPE;
CREATE TABLE MUSIT_EVENT.EVENT_TYPE(
event_type_id SERIAL PRIMARY KEY,
event_type TEXT NOT NULL,
event_type_for TEXT,
default_object_role INTEGER,
default_person_role INTEGER,
display_name TEXT
);

drop table if exists MUSIT_EVENT.ROLE_FOR_EVENTTYPE;
CREATE TABLE MUSIT_EVENT.ROLE_FOR_EVENTTYPE(
ROLE_ID INTEGER NOT NULL,
EVENT_TYPE_ID INTEGER NOT NULL,
PRIMARY KEY (ROLE_ID,EVENT_TYPE_ID),
FOREIGN KEY(ROLE_ID) REFERENCES MUSIT_EVENT.ROLE(ROLE_ID),
FOREIGN KEY(EVENT_TYPE_ID) REFERENCES MUSIT_EVENT.EVENT_TYPE(EVENT_TYPE_ID)
);


drop table if exists MUSIT_PERSON.ATTRIBUTE;
CREATE TABLE MUSIT_PERSON.ATTRIBUTE(
attribute_uuid UUID NOT NULL,
title TEXT,
legal_entity_type TEXT NOT NULL,
date_birth date,
date_dead date,
url TEXT,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (attribute_uuid)
);


drop table if exists MUSIT_PERSON.APPELLATION_PERSON_NAME;
CREATE TABLE MUSIT_PERSON.APPELLATION_PERSON_NAME(
person_name_uuid UUID NOT NULL,
first_name TEXT,
last_name TEXT,
name TEXT,
display_name TEXT,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (person_name_uuid)
);

drop table if exists MUSIT_EVENT.EVENT;
CREATE TABLE MUSIT_EVENT.EVENT(
event_id BIGSERIAL NOT NULL,
event_uuid UUID PRIMARY KEY,
event_type_id INTEGER,
museum_id INTEGER,
note TEXT,
part_of UUID,
is_deleted BOOLEAN DEFAULT FALSE,
updated_by UUID,
updated_date timestamp with time zone,
registered_by UUID,
registered_date timestamp with time zone,
event_json JSONB,
event_date_from date,
event_date_to date,
event_date_verbatim text,
person_attribute_uuid uuid,
person_name_uuid uuid,
FOREIGN KEY (event_type_id) REFERENCES MUSIT_EVENT.EVENT_TYPE (event_type_id),
FOREIGN KEY (person_attribute_uuid) REFERENCES MUSIT_PERSON.ATTRIBUTE(attribute_uuid),
FOREIGN KEY (person_name_uuid) REFERENCES MUSIT_PERSON.APPELLATION_PERSON_NAME(person_name_uuid)
);



drop table if exists MUSIT_PERSON.PERSON;
CREATE TABLE MUSIT_PERSON.PERSON(
person_uuid UUID NOT NULL,
display_name TEXT,
latest_appellation_event_uuid UUID,
latest_attribute_event_uuid UUID,
current_person_uuid UUID,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (person_uuid),
FOREIGN KEY (latest_appellation_event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid),
FOREIGN KEY (latest_attribute_event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid)
);

drop table if exists MUSIT_PERSON.BRUKER;
CREATE TABLE MUSIT_PERSON.BRUKER(
feide_uuid UUID NOT NULL,
user_name TEXT,
current_person_uuid UUID NOT NULL,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (feide_uuid),
FOREIGN KEY (current_person_uuid) REFERENCES MUSIT_PERSON.PERSON(person_uuid)
);


drop table if exists MUSIT_EVENT.EVENT_ROLE_PERSON_NAME;
CREATE TABLE MUSIT_EVENT.EVENT_ROLE_PERSON_NAME(
event_uuid UUID NOT NULL,
role_id INTEGER NOT NULL,
person_name_uuid UUID NOT NULL,
name TEXT NOT NULL,
person_uuid UUID,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (event_uuid,role_id, person_name_uuid),
FOREIGN KEY (person_uuid) REFERENCES MUSIT_PERSON.PERSON (person_uuid),
FOREIGN KEY (person_name_uuid) REFERENCES MUSIT_PERSON.APPELLATION_PERSON_NAME(person_name_uuid),
FOREIGN KEY (event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid),
FOREIGN KEY (role_id) REFERENCES MUSIT_EVENT.ROLE(role_id)
);

drop table if exists MUSIT_EVENT.EVENT_ROLE_OBJECT;
CREATE TABLE MUSIT_EVENT.EVENT_ROLE_OBJECT(
event_uuid UUID NOT NULL,
role_id INTEGER NOT NULL,
object_uuid UUID NOT NULL,
PRIMARY KEY (event_uuid,role_id, object_uuid),
--FOREIGN KEY (object_uuid) REFERENCES MUSIT_THING.OBJECT (object_uuid),
FOREIGN KEY (event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid),
FOREIGN KEY (role_id) REFERENCES MUSIT_EVENT.ROLE(role_id)
);

drop table if exists MUSIT_EVENT.EVENT_ROLE_PLACE;
CREATE TABLE MUSIT_EVENT.EVENT_ROLE_PLACE(
event_uuid UUID NOT NULL,
role_id INTEGER NOT NULL,
place_uuid UUID NOT NULL,
PRIMARY KEY (place_uuid, event_uuid,role_id),
FOREIGN KEY (place_uuid) REFERENCES MUSIT_PLACE.PLACE(place_uuid),
FOREIGN KEY (event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid),
FOREIGN KEY (role_id) REFERENCES MUSIT_EVENT.ROLE(role_id)
);



drop table if exists MUSIT_EVENT.MUSEUM;
CREATE TABLE MUSIT_EVENT.MUSEUM(
museum_id INTEGER PRIMARY KEY,
museum_name TEXT NOT NULL,
abbr TEXT
);

drop table if exists MUSIT_EVENT.COLLECTION;
CREATE TABLE MUSIT_EVENT.COLLECTION(
collection_id INTEGER PRIMARY KEY,
collection_uuid UUID,
collection_name TEXT NOT NULL
);

drop table if exists MUSIT_EVENT.MUSEUM_COLLECTION;
CREATE TABLE MUSIT_EVENT.MUSEUM_COLLECTION(
mus_coll_id SERIAL PRIMARY KEY,
museum_id INTEGER NOT NULL,
collection_id INTEGER NOT NULL,
UNIQUE (museum_id, collection_id),
FOREIGN KEY (museum_id) REFERENCES MUSIT_EVENT.MUSEUM (museum_id),
FOREIGN KEY (collection_id) REFERENCES MUSIT_EVENT.COLLECTION (collection_id)
);


drop table if exists MUSIT_EVENT.PERSON_MUSEUM_COLLECTION;
CREATE TABLE MUSIT_EVENT.PERSON_MUSEUM_COLLECTION(
person_uuid UUID NOT NULL,
mus_coll_id INTEGER NOT NULL,
PRIMARY KEY (person_uuid, mus_coll_id),
FOREIGN KEY (person_uuid) REFERENCES MUSIT_PERSON.PERSON (person_uuid),
FOREIGN KEY (mus_coll_id) REFERENCES MUSIT_EVENT.MUSEUM_COLLECTION(mus_coll_id)
);



