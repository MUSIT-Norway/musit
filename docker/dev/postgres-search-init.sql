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


drop table if exists MUSIT_EVENT.MUSEUM;
CREATE TABLE MUSIT_EVENT.MUSEUM(
museum_id INTEGER PRIMARY KEY,
museum_name TEXT NOT NULL,
abbreviation TEXT
);

drop table if exists MUSIT_EVENT.COLLECTION;
CREATE TABLE MUSIT_EVENT.COLLECTION(
collection_id INTEGER PRIMARY KEY,
collection_uuid UUID,
collection_name TEXT NOT NULL
);

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


/*denne tabellen ses på som en referansetabell over navn vi har i systemet. Det linkes opp
til personer via eventer som sier noe om navnet knyttet til person*/
drop table if exists MUSIT_PERSON.APPELLATION_PERSON_NAME;
CREATE TABLE MUSIT_PERSON.APPELLATION_PERSON_NAME(
person_name_uuid UUID NOT NULL,
first_name TEXT,
last_name TEXT,
name TEXT NOT NULL,
title TEXT,
is_deleted BOOLEAN DEFAULT FALSE,
concat_person_name TEXT,
PRIMARY KEY (person_name_uuid)
);
COMMENT ON COLUMN MUSIT_PERSON.APPELLATION_PERSON_NAME.name
IS 'aggregated like this: lastname, title firstname or just name if there is no firstname and lastname';

drop table if exists MUSIT_EVENT.EVENT;
CREATE TABLE MUSIT_EVENT.EVENT(
event_id BIGSERIAL NOT NULL,
event_uuid UUID PRIMARY KEY,
event_type_id INTEGER,
museum_id INTEGER,
collection_id INTEGER,
note TEXT,
part_of UUID,
is_deleted BOOLEAN DEFAULT FALSE,
created_by UUID,
created_date timestamp with time zone,
event_json JSONB,
event_date_from timestamp with time zone,
event_date_to timestamp with time zone,
event_date_verbatim text,
FOREIGN KEY (event_type_id) REFERENCES MUSIT_EVENT.EVENT_TYPE (event_type_id),
FOREIGN KEY (museum_id) REFERENCES MUSIT_EVENT.MUSEUM (museum_id),
FOREIGN KEY (collection_id) REFERENCES MUSIT_EVENT.COLLECTION (collection_id)
);

drop table if exists MUSIT_EVENT.EVENT_ROLE_EVENT;
CREATE TABLE MUSIT_EVENT.EVENT_ROLE_EVENT(
event_Uuid UUID,
role_id  INTEGER NOT NULL,
to_event_uuid UUID NOT NULL,
PRIMARY KEY (EVENT_UUID,ROLE_ID, TO_EVENT_UUID),
FOREIGN KEY(ROLE_ID) REFERENCES MUSIT_EVENT.ROLE(ROLE_ID),
FOREIGN KEY(EVENT_UUID) REFERENCES MUSIT_EVENT.EVENT(EVENT_UUID),
FOREIGN KEY(TO_EVENT_UUID) REFERENCES MUSIT_EVENT.EVENT(EVENT_UUID)
);

COMMENT ON COLUMN MUSIT_EVENT.EVENT_ROLE_EVENT.EVENT_UUID
IS 'this is the current event that owns the relation to the to_event_uuid. eventUuid is subevent to to_event_uuid,
or eventUuid has deleted_synonym on to_event_uuid';


/*denne tabellen er en sub-type av event for person_navn (rediger_navn_eventtype)*/
drop table if exists MUSIT_PERSON.EVENT_PERSON_NAME;
CREATE TABLE MUSIT_PERSON.EVENT_PERSON_NAME(
event_uuid UUID NOT NULL,
person_name_uuid UUID,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (event_uuid,person_name_uuid),
 FOREIGN KEY(EVENT_UUID) REFERENCES MUSIT_EVENT.EVENT(EVENT_UUID),
 FOREIGN KEY(person_name_uuid) REFERENCES MUSIT_PERSON.APPELLATION_PERSON_NAME(person_name_uuid)
);

drop table if exists MUSIT_PERSON.ATTRIBUTE;
CREATE TABLE MUSIT_PERSON.ATTRIBUTE(
event_uuid UUID NOT NULL,
legal_entity_type TEXT NOT NULL,
date_birth TIMESTAMPTZ,
date_dead TIMESTAMPTZ,
date_verbatim TEXT,
url TEXT,
external_Ids JSON,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (event_uuid),
FOREIGN KEY(EVENT_UUID) REFERENCES MUSIT_EVENT.EVENT(EVENT_UUID)
);

COMMENT ON COLUMN MUSIT_PERSON.ATTRIBUTE.legal_entity_type
IS 'which type of person is this, person, organization, institution etc';
COMMENT ON COLUMN MUSIT_PERSON.ATTRIBUTE.external_Ids
IS 'external IDs to other datatbases. It is JSON in this column for avoiding another table for it';


drop table if exists MUSIT_PERSON.PERSON;
CREATE TABLE MUSIT_PERSON.PERSON(
person_uuid UUID NOT NULL,
display_name TEXT,
latest_edit_event_uuid UUID,
museum_id INTEGER,
collection_id INTEGER,
latest_attribute_event_uuid UUID,
latest_synonym_event_uuid UUID,
current_person_uuid UUID,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (person_uuid),
FOREIGN KEY (latest_edit_event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid),
FOREIGN KEY (latest_attribute_event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid),
FOREIGN KEY (latest_synonym_event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid),
FOREIGN KEY (museum_id) REFERENCES MUSIT_EVENT.MUSEUM (museum_id),
FOREIGN KEY (collection_id) REFERENCES MUSIT_EVENT.COLLECTION (collection_id)
);


COMMENT ON COLUMN MUSIT_PERSON.PERSON.display_name
IS 'display name for this person';
COMMENT ON COLUMN  MUSIT_PERSON.PERSON.latest_edit_event_uuid
IS 'the eventUuid for the latest event for editing the persons name';
COMMENT ON COLUMN MUSIT_PERSON.PERSON.latest_attribute_event_uuid
IS 'the eventUuid for the latest event that changed some of the attributes for this person';
COMMENT ON COLUMN  MUSIT_PERSON.PERSON.latest_synonym_event_uuid
IS 'the eventUuid for the latest event for editing synonyms';
COMMENT ON COLUMN  MUSIT_PERSON.PERSON.current_person_uuid
IS 'personUuid for the person that is the current person if two or more persons are merged';

drop table if exists MUSIT_PERSON.USERS;
CREATE TABLE MUSIT_PERSON.USERS(
feide_uuid UUID NOT NULL,
user_name TEXT,
current_person_uuid UUID NOT NULL,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (feide_uuid),
FOREIGN KEY (current_person_uuid) REFERENCES MUSIT_PERSON.PERSON(person_uuid)
);

drop table if exists MUSIT_PERSON.AGGREGATION_SEARCH;
CREATE TABLE MUSIT_PERSON.AGGREGATION_SEARCH(
aggSearch_id BIGSERIAL NOT NULL,
person_name_uuid UUID NOT NULL,
first_name TEXT,
last_name TEXT,
name TEXT,
display_name TEXT,
concat_person_name TEXT,
title TEXT,
person_uuid UUID,
legal_entity_type TEXT,
date_birth TIMESTAMPTZ,
date_dead TIMESTAMPTZ,
date_verbatim TEXT,
url TEXT,
external_Ids JSON,
latest_edited_name_uuid UUID,
PRIMARY KEY (aggSearch_id),
FOREIGN KEY (person_name_uuid) REFERENCES MUSIT_PERSON.APPELLATION_PERSON_NAME(person_name_uuid),
FOREIGN KEY (latest_edited_name_uuid) REFERENCES MUSIT_PERSON.APPELLATION_PERSON_NAME(person_name_uuid)
);
COMMENT ON COLUMN  MUSIT_PERSON.AGGREGATION_SEARCH.latest_edited_name_uuid
IS 'which person_name-row that shows the latest edited name for a person';


drop table if exists MUSIT_EVENT.EVENT_ROLE_PERSON_NAME;
CREATE TABLE MUSIT_EVENT.EVENT_ROLE_PERSON_NAME(
erp_id BIGSERIAL NOT NULL,
event_uuid UUID NOT NULL,
role_id INTEGER NOT NULL,
person_name_uuid UUID NOT NULL,
name TEXT NOT NULL,
person_uuid UUID,
is_deleted BOOLEAN DEFAULT FALSE,
PRIMARY KEY (erp_id),
FOREIGN KEY (person_uuid) REFERENCES MUSIT_PERSON.PERSON (person_uuid),
FOREIGN KEY (person_name_uuid) REFERENCES MUSIT_PERSON.APPELLATION_PERSON_NAME(person_name_uuid),
FOREIGN KEY (event_uuid) REFERENCES MUSIT_EVENT.EVENT(event_uuid),
FOREIGN KEY (role_id) REFERENCES MUSIT_EVENT.ROLE(role_id),
UNIQUE (event_uuid, role_id, person_Name_uuid)
);
COMMENT ON COLUMN  MUSIT_EVENT.EVENT_ROLE_PERSON_NAME.name
IS 'the original name(the right synonym) of the person. Cashed data for the personUuid';
COMMENT ON COLUMN  MUSIT_EVENT.EVENT_ROLE_PERSON_NAME.erp_id
IS 'a sequence since we have several versions of the same relation, so problem with PK';


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



