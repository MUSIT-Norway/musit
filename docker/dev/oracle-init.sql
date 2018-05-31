CREATE USER musit IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
GRANT DBA TO musit;
GRANT UNLIMITED TABLESPACE TO musit;
ALTER USER musit QUOTA UNLIMITED ON USERS;

CREATE USER musark_auth IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
CREATE USER musark_core IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
CREATE USER musark_storage IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
CREATE USER musark_actor IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
CREATE USER musark_analysis IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
CREATE USER musark_loan IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
CREATE USER musit_mapping IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
CREATE USER musark_conservation IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;
CREATE USER musark_thing IDENTIFIED BY musit DEFAULT TABLESPACE USERS PROFILE DEFAULT ACCOUNT UNLOCK;

ALTER USER musark_auth QUOTA UNLIMITED ON USERS;
ALTER USER musark_core QUOTA UNLIMITED ON USERS;
ALTER USER musark_storage QUOTA UNLIMITED ON USERS;
ALTER USER musark_actor QUOTA UNLIMITED ON USERS;
ALTER USER musark_analysis QUOTA UNLIMITED ON USERS;
ALTER USER musark_loan QUOTA UNLIMITED ON USERS;
ALTER USER musit_mapping QUOTA UNLIMITED ON USERS;
ALTER USER musark_conservation QUOTA UNLIMITED ON USERS;
ALTER USER musark_thing QUOTA UNLIMITED ON USERS;

-- Create Auth tables
CREATE TABLE MUSARK_AUTH.AUTH_GROUP (
  group_uuid        VARCHAR2(36) NOT NULL,
  group_name        VARCHAR(100) NOT NULL,
  group_module      INTEGER      NOT NULL,
  group_permission  INTEGER      NOT NULL,
  group_museumId    INTEGER      NOT NULL,
  group_description VARCHAR(512),
  PRIMARY KEY (group_uuid),
  CONSTRAINT unique_group_name UNIQUE (group_name)
);

CREATE TABLE MUSARK_AUTH.MUSEUM_COLLECTION (
  collection_uuid               VARCHAR2(36) NOT NULL,
  collection_name               VARCHAR(100),
  collection_schema_identifiers VARCHAR(100) NOT NULL,
  PRIMARY KEY (collection_uuid),
  CONSTRAINT unique_mcol_name UNIQUE (collection_name)
);

CREATE TABLE MUSARK_AUTH.USER_AUTH_GROUP (
  uag_id           INTEGER GENERATED BY DEFAULT AS IDENTITY,
  user_feide_email VARCHAR(254) NOT NULL,
  group_uuid       VARCHAR2(36) NOT NULL,
  collection_uuid  VARCHAR(36),
  PRIMARY KEY (uag_id),
  FOREIGN KEY (group_uuid) REFERENCES MUSARK_AUTH.AUTH_GROUP (group_uuid),
  FOREIGN KEY (collection_uuid) REFERENCES MUSARK_AUTH.MUSEUM_COLLECTION (collection_uuid)
);

CREATE TABLE MUSARK_AUTH.USER_INFO (
  user_uuid    VARCHAR2(36) NOT NULL,
  secondary_id VARCHAR(512),
  name         VARCHAR(512),
  email        VARCHAR(254),
  picture      VARCHAR(100),
  PRIMARY KEY (user_uuid)
);

-- Table for keeping tabs on a users authenticated activity. "user_uuid" is not
-- set as a foreign key deliberately. This allows us to prepare the session with
-- a unique ID before we've received the access token from Dataporten.
CREATE TABLE MUSARK_AUTH.USER_SESSION (
  session_uuid     VARCHAR2(36)      NOT NULL,
  token            VARCHAR2(36),
  user_uuid        VARCHAR2(36),
  login_time       TIMESTAMP WITH TIME ZONE,
  last_active      TIMESTAMP WITH TIME ZONE,
  is_logged_in     INTEGER DEFAULT 0 NOT NULL,
  token_expires_in NUMBER(20),
  client           VARCHAR2(20),
  PRIMARY KEY (session_uuid)
);

-- Core tables
CREATE TABLE MUSARK_CORE.ES_INDEX_STATUS (
  index_alias   VARCHAR2(255)            NOT NULL,
  index_created TIMESTAMP WITH TIME ZONE NOT NULL,
  index_updated TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (index_alias)
);

-- Actor tables
CREATE TABLE MUSIT_MAPPING.ACTOR (
  actorid             NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  actorname           VARCHAR2(512),
  dataporten_uuid     VARCHAR2(36),
  dataporten_username VARCHAR(100),
  old_username        VARCHAR2(50),
  lokal_pk            INTEGER,
  tabellid            INTEGER,
  old_schemaname      VARCHAR2(50),
  museum_id           INTEGER,
  application_uuid    VARCHAR2(36),
  PRIMARY KEY (actorid)
);

CREATE TABLE MUSARK_ACTOR.ORGANISATION (
  org_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  full_name VARCHAR(255) NOT NULL,
  tel VARCHAR(50),
  web VARCHAR(255),
  synonyms VARCHAR(255),
  service_tags VARCHAR(500),
  contact VARCHAR2(255),
  email VARCHAR2(255),
  PRIMARY KEY (org_id)
);

CREATE TABLE MUSARK_ACTOR.ORGANISATION_ADDRESS (
  orgaddressid NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  org_id NUMBER(20) NOT NULL,
  street_address VARCHAR(100),
  street_address_2 VARCHAR2(255),
  postal_code_place VARCHAR(50),
  country_name VARCHAR(255),
  PRIMARY KEY (orgaddressid),
  FOREIGN KEY (org_id) REFERENCES MUSARK_ACTOR.ORGANISATION(org_id)
);


-- storage facility tables
CREATE TABLE MUSARK_STORAGE.STORAGE_NODE (
  storage_node_id   NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  storage_node_uuid VARCHAR2(36), -- FIXME: This field should definitely be NOT NULL
  storage_node_name VARCHAR(512),
  area              NUMBER,
  area_to           NUMBER,
  is_storage_unit   VARCHAR(1)   DEFAULT '1',
  is_part_of        NUMBER(20),
  height            NUMBER,
  height_to         NUMBER,
  node_path         VARCHAR(1000)            NOT NULL,
  is_deleted        INTEGER DEFAULT 0        NOT NULL,
  storage_type      VARCHAR(100) DEFAULT 'StorageUnit',
  group_read        VARCHAR(4000),
  group_write       VARCHAR(4000),
  old_barcode       NUMBER(20),
  museum_id         INTEGER                  NOT NULL,
  updated_by        VARCHAR2(36)             NOT NULL,
  updated_date      TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.ROOM (
  storage_node_id               NUMBER(20) NOT NULL,
  perimeter_security            INTEGER,
  theft_protection              INTEGER,
  fire_protection               INTEGER,
  water_damage_assessment       INTEGER,
  routines_and_contingency_plan INTEGER,
  relative_humidity             INTEGER,
  temperature_assessment        INTEGER,
  lighting_condition            INTEGER,
  preventive_conservation       INTEGER,
  PRIMARY KEY (storage_node_id),
  FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE (storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.BUILDING (
  storage_node_id NUMBER(20) NOT NULL,
  postal_address  VARCHAR(512),
  PRIMARY KEY (storage_node_id),
  FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE (storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.ORGANISATION (
  storage_node_id NUMBER(20) NOT NULL,
  postal_address  VARCHAR(512),
  org_as_actor_id NUMBER(20),
  org_as_actor_uuid VARCHAR2(36),
  PRIMARY KEY (storage_node_id),
  FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE (storage_node_id)
);

-- ===========================================================================
-- The NEW StorageFacility event tables
-- ===========================================================================
CREATE SEQUENCE MUSARK_STORAGE.nevent_sequence
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;

CREATE TABLE MUSARK_STORAGE.NEW_EVENT (
  event_id        NUMBER(20) DEFAULT MUSARK_STORAGE.nevent_sequence.nextval,
  type_id         INTEGER             NOT NULL,
  museum_id       INTEGER                  NOT NULL,
  done_by         VARCHAR2(512),
  done_date       TIMESTAMP WITH TIME ZONE,
  updated_date    TIMESTAMP WITH TIME ZONE,
  registered_by   VARCHAR2(36)             NOT NULL,
  registered_date TIMESTAMP WITH TIME ZONE NOT NULL,
  part_of         NUMBER(20),
  affected_uuid   VARCHAR2(36),
  affected_type   VARCHAR2(50), -- collection | sample | node | ...
  note            VARCHAR2(500),
  event_json      CLOB,
  PRIMARY KEY (event_id),
  CONSTRAINT ensure_storage_event_json CHECK (event_json IS JSON)
);

CREATE TABLE MUSARK_STORAGE.NEW_LOCAL_OBJECT (
  object_uuid         VARCHAR2(36) NOT NULL,
  latest_move_id      NUMBER(20)   NOT NULL,
  current_location_id VARCHAR2(36) NOT NULL,
  museum_id           INTEGER      NOT NULL,
  object_type         VARCHAR(50) DEFAULT 'collection', -- possible values can be 'collection', or 'sample'
  PRIMARY KEY (object_uuid),
  FOREIGN KEY (latest_move_id) REFERENCES MUSARK_STORAGE.NEW_EVENT (event_id)
);

CREATE TABLE MUSARK_STORAGE.EVENT_TYPE (
  event_type_id INTEGER GENERATED BY DEFAULT AS IDENTITY,
  name          VARCHAR(100) NOT NULL,
  PRIMARY KEY (event_type_ID)
);

-- ===========================================================================
-- Temporary mapping table for museum objects.
-- ===========================================================================

-- Object table
CREATE TABLE MUSIT_MAPPING.MUSITTHING (
  object_id         NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  museumNo          VARCHAR(100)      NOT NULL,
  subNo             VARCHAR(100),
  term              VARCHAR(256)      NOT NULL,
  museumId          INTEGER           NOT NULL,
  museumNoAsNumber  INTEGER,
  subNoAsNumber     INTEGER,
  mainobject_id     NUMBER(20),
  is_deleted        INTEGER DEFAULT 0 NOT NULL,
  old_schemaname    VARCHAR(100),
  lokal_pk          NUMBER(20),
  old_barcode       INTEGER,
  new_collection_id INTEGER,
  musitthing_uuid   VARCHAR(36),
  updated_date      TIMESTAMP NOT NULL,
  ark_form          VARCHAR2(2000),
  ark_funn_nr       VARCHAR2(500 CHAR),
  nat_stage         VARCHAR2(256),
  nat_gender        VARCHAR2(256),
  nat_legdato       VARCHAR2(64),
  num_denotation    VARCHAR2(255),
  num_valor         VARCHAR2(100),
  num_date          VARCHAR2(50),
  num_weight        VARCHAR2(100),
  aggregated_class_data VARCHAR2(500),
  PRIMARY KEY (object_id)
);


-- ===========================================================================
-- Temporary mapping table for museum object's material.
-- ===========================================================================

CREATE TABLE MUSIT_MAPPING.THING_MATERIAL
(
  collectionid         INTEGER,
  objectid             INTEGER,
  etn_materialtype     VARCHAR2(100),
  etn_material         VARCHAR2(300),
  etn_material_element VARCHAR2(100),
  etn_matrid_local     INTEGER,
  ark_material         VARCHAR2(500),
  ark_spes_material    VARCHAR2(500),
  ark_sortering        INTEGER,
  ark_hid_local        INTEGER,
  num_material         VARCHAR2(100),
  num_numistypeid      INTEGER
);

-- ===========================================================================
-- Temporary mapping table for museum object's location.
-- ===========================================================================
create table MUSIT_MAPPING.THING_LOCATION
(
  collectionid       INTEGER,
  objectid           INTEGER,
  ark_gardsnavn      VARCHAR2(100),
  ark_gardsnr        INTEGER,
  ark_bruksnr        VARCHAR2(100),

  ark_stedid         INTEGER,
  nat_country        VARCHAR2(100),
  nat_state_province VARCHAR2(100),
  nat_municipality   VARCHAR2(100),
  nat_locality       VARCHAR2(4000),
  nat_coordinate     VARCHAR2(256),
  nat_coord_datum    VARCHAR2(64),
  nat_sone_band      VARCHAR2(16),
  etn_place          VARCHAR2(250),
  etn_country        VARCHAR2(100),
  etn_region1        VARCHAR2(100),
  etn_region2        VARCHAR2(100),
  etn_area           VARCHAR2(100),
  etn_local_stedid   INTEGER,
  etn_place_count    INTEGER
);

-- ===========================================================================
-- Temporary mapping table for museum object's coordinate.
-- ===========================================================================
CREATE TABLE MUSIT_MAPPING.THING_COORDINATE
(
  collectionid       INTEGER,
  objectid           INTEGER,
  ark_projeksjon     VARCHAR2(100),
  ark_presisjon      VARCHAR2(100),
  ark_nord           VARCHAR2(50),
  ark_ost            VARCHAR2(50),
  ark_localksettid   INTEGER
);

-- ===========================================================================
-- Temporary mapping table for museum object's coordinate.
-- ===========================================================================

CREATE TABLE MUSARK_THING.MUSITTHING_SEARCH
(
  objectuuid            VARCHAR2(36) not null,
  museumno              VARCHAR2(1000),
  subno                 VARCHAR2(500),
  term                  VARCHAR2(500),
  mainobject_id         INTEGER,
  new_collection_id     INTEGER,
  ark_form              VARCHAR2(2000),
  ark_funn_nr           VARCHAR2(500 CHAR),
  nat_stage             VARCHAR2(256),
  nat_gender            VARCHAR2(256),
  nat_legdato           VARCHAR2(64),
  is_deleted            INTEGER default 0,
  aggregated_class_data VARCHAR2(4000),
  updated_date          TIMESTAMP(6) not null,
  museumno_prefix       VARCHAR2(20),
  museumno_number       INTEGER,
  subno_number          INTEGER,
  museumid              INTEGER,
  document_json         VARCHAR2(4000),
   PRIMARY KEY (objectuuid)
);

CREATE TABLE MUSARK_THING.MUSITTHING_SEARCH_POPULATING
(
  objectuuid            VARCHAR2(36) not null,
  museumno              VARCHAR2(1000),
  subno                 VARCHAR2(500),
  term                  VARCHAR2(500),
  mainobject_id         INTEGER,
  new_collection_id     INTEGER,
  ark_form              VARCHAR2(2000),
  ark_funn_nr           VARCHAR2(500 CHAR),
  nat_stage             VARCHAR2(256),
  nat_gender            VARCHAR2(256),
  nat_legdato           VARCHAR2(64),
  is_deleted            INTEGER default 0,
  aggregated_class_data VARCHAR2(4000),
  updated_date          TIMESTAMP(6) not null,
  museumno_prefix       VARCHAR2(20),
  museumno_number       INTEGER,
  subno_number          INTEGER,
  museumid              INTEGER,
  document_json         VARCHAR2(4000),
   PRIMARY KEY (objectuuid)
);

-- ===========================================================================
-- Tables for Analysis and SampleObject management
-- ===========================================================================
CREATE SEQUENCE MUSARK_ANALYSIS.sample_object_sample_num_seq
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;

CREATE TABLE MUSARK_ANALYSIS.SAMPLE_OBJECT (
  sample_uuid            VARCHAR2(36)             NOT NULL,
  parent_object_uuid     VARCHAR2(36),
  parent_object_type     VARCHAR(50),
  is_extracted           INTEGER DEFAULT 0        NOT NULL,
  museum_id              INTEGER                  NOT NULL,
  status                 INTEGER DEFAULT 1        NOT NULL,
  responsible_actor      VARCHAR2(512),
  done_by                VARCHAR2(36),
  done_date              TIMESTAMP WITH TIME ZONE,
  sample_id              VARCHAR2(100),
  sample_num             INTEGER DEFAULT MUSARK_ANALYSIS.sample_object_sample_num_seq.nextval,
  external_id            VARCHAR2(100),
  external_id_source     VARCHAR2(100),
  sample_type_id         INTEGER                  NOT NULL,
  sample_size            NUMBER,
  sample_size_unit       VARCHAR2(10),
  sample_container       VARCHAR2(100),
  storage_medium         VARCHAR2(100),
  treatment              VARCHAR2(100),
  leftover_sample        INTEGER DEFAULT 1        NOT NULL,
  description            VARCHAR2(250),
  note                   VARCHAR2(250),
  originated_object_uuid VARCHAR2(36)             NOT NULL,
  registered_by          VARCHAR2(36)             NOT NULL,
  registered_date        TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_by             VARCHAR2(36),
  updated_date           TIMESTAMP WITH TIME ZONE,
  is_deleted             INTEGER DEFAULT 0        NOT NULL,
  PRIMARY KEY (sample_uuid)
);


CREATE TABLE MUSARK_ANALYSIS.SAMPLE_TYPE (
  sampletype_id    NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  no_sampletype    VARCHAR2(100) NOT NULL,
  en_sampletype    VARCHAR2(100) NOT NULL,
  no_samplesubtype VARCHAR2(100),
  en_samplesubtype VARCHAR2(100),
  PRIMARY KEY (sampletype_id)
);

CREATE SEQUENCE MUSARK_ANALYSIS.event_type_type_id_seq
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;

CREATE TABLE MUSARK_ANALYSIS.EVENT_TYPE (
  type_id     INTEGER DEFAULT MUSARK_ANALYSIS.event_type_type_id_seq.nextval,
  category                     INTEGER       NOT NULL,
  no_name                      VARCHAR2(100) NOT NULL,
  en_name                      VARCHAR2(100) NOT NULL,
  short_name                   VARCHAR2(50),
  collections                  VARCHAR2(500), -- if empty then all collections, else value is ',uuid_1,uuid_2,uuid_9,'     LIKE '%,uuid_2,%'
  extra_description_type       VARCHAR2(50),
  extra_description_attributes CLOB,
  extra_result_type            VARCHAR2(50),
  extra_result_attributes      CLOB,
  PRIMARY KEY (type_id)
);

CREATE TABLE MUSARK_ANALYSIS.EVENT (
  event_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  type_id         INTEGER                  NOT NULL,
  museum_id       INTEGER                  NOT NULL,
  done_by         VARCHAR2(512),
  done_date       TIMESTAMP WITH TIME ZONE,
  updated_date    TIMESTAMP WITH TIME ZONE,
  registered_by   VARCHAR2(36)             NOT NULL,
  registered_date TIMESTAMP WITH TIME ZONE NOT NULL,
  part_of         NUMBER(20),
  affected_uuid   VARCHAR2(36),
  note            VARCHAR2(500),
  status          INTEGER,
  case_numbers    VARCHAR2(1000),
  event_json      CLOB,
  PRIMARY KEY (event_id),
  CONSTRAINT ensure_event_json CHECK (event_json IS JSON)
);

CREATE TABLE MUSARK_ANALYSIS.RESULT (
  event_id        NUMBER(20)               NOT NULL,
  museum_id       INTEGER                  NOT NULL,
  registered_by   VARCHAR2(36)             NOT NULL,
  registered_date TIMESTAMP WITH TIME ZONE NOT NULL,
  result_json     CLOB,
  PRIMARY KEY (event_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_ANALYSIS.EVENT (event_id),
  CONSTRAINT ensure_result_json CHECK (result_json IS JSON)
);

CREATE TABLE MUSARK_ANALYSIS.TREATMENT (
  treatment_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  no_treatment VARCHAR2(100) NOT NULL,
  en_treatment VARCHAR2(100) NOT NULL,
  PRIMARY KEY (treatment_id)
);


CREATE TABLE MUSARK_ANALYSIS.STORAGEMEDIUM (
  storagemedium_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  no_storagemedium VARCHAR2(100) NOT NULL,
  en_storagemedium VARCHAR2(100) NOT NULL,
  PRIMARY KEY (storagemedium_id)
);

CREATE TABLE MUSARK_ANALYSIS.STORAGECONTAINER (
  storagecontainer_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  no_storagecontainer VARCHAR2(100) NOT NULL,
  en_storagecontainer VARCHAR2(100) NOT NULL,
  PRIMARY KEY (storagecontainer_id)
);


CREATE TABLE MUSARK_LOAN.LOAN_EVENT (
  event_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  type_id         INTEGER                  NOT NULL,
  event_date      TIMESTAMP WITH TIME ZONE NOT NULL,
  registered_by   VARCHAR2(36)             NOT NULL,
  registered_date TIMESTAMP WITH TIME ZONE NOT NULL,
  museum_id       INTEGER                  NOT NULL,
  part_of         NUMBER(20),
  object_uuid     VARCHAR2(36),
  case_numbers    VARCHAR2(1000),
  note            VARCHAR2(500),
  event_json      CLOB,
  PRIMARY KEY (event_id),
  CONSTRAINT ensure_event_json CHECK (event_json IS JSON)
);

CREATE TABLE MUSARK_LOAN.ACTIVE_LOAN (
  active_loan_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  museum_id      INTEGER                  NOT NULL,
  object_uuid    VARCHAR2(36)             NOT NULL UNIQUE,
  event_id       NUMBER(20)               NOT NULL,
  return_date    TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (active_loan_id)
);

CREATE TABLE MUSARK_LOAN.LENT_OBJECT (
  lent_object_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  event_id       NUMBER(20)   NOT NULL,
  object_uuid    VARCHAR2(36) NOT NULL,
  PRIMARY KEY (lent_object_id)
);


CREATE SEQUENCE MUSARK_CONSERVATION.event_eventid_seq
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;



CREATE TABLE MUSARK_CONSERVATION.EVENT (
  event_id        NUMBER(20) DEFAULT MUSARK_CONSERVATION.event_eventid_seq.nextval,
  type_id         INTEGER                  NOT NULL,
  museum_id       INTEGER                  NOT NULL,
  updated_by      VARCHAR2(36),
  updated_date    TIMESTAMP WITH TIME ZONE,
  registered_by   VARCHAR2(36)             NOT NULL,
  registered_date TIMESTAMP WITH TIME ZONE NOT NULL,
  part_of         NUMBER(20),
  affected_uuid   VARCHAR2(36),
  note            VARCHAR2(4000),
  case_number     VARCHAR2(1000),
  event_json      CLOB,
  is_deleted      INTEGER DEFAULT 0 NOT NULL,
  PRIMARY KEY (event_id),
  CONSTRAINT ensure_event_json CHECK (event_json IS JSON)
  );


CREATE SEQUENCE MUSARK_CONSERVATION.event_type_typeid_seq
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;

CREATE TABLE MUSARK_CONSERVATION.EVENT_TYPE (
  type_id     INTEGER DEFAULT MUSARK_CONSERVATION.event_type_typeid_seq.nextval,
  no_name                      VARCHAR2(100) NOT NULL,
  en_name                      VARCHAR2(100) NOT NULL,
  collections                  VARCHAR2(500), -- if empty then all collections, else value is ',uuid_1,uuid_2,uuid_9,'     LIKE '%,uuid_2,%'
  extra_description_type       VARCHAR2(50),
  extra_description_attributes CLOB,
  PRIMARY KEY (type_id)
);


CREATE TABLE MUSARK_CONSERVATION.OBJECT_EVENT(
object_uuid varchar2(36)NOT NULL,
event_id number(20) NOT NULL,
PRIMARY KEY (object_uuid,event_id)
);


CREATE SEQUENCE MUSARK_CONSERVATION.ROLE_SEQ
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;

CREATE TABLE MUSARK_CONSERVATION.ROLE(
role_id INTEGER DEFAULT MUSARK_CONSERVATION.ROLE_SEQ.NEXTVAL,
no_role VARCHAR2(100),
en_role VARCHAR2(100),
role_for VARCHAR2(50), -- om det er rollen hendelse_aktør eller hendelse_gjenstand
PRIMARY KEY (role_id)
);

CREATE TABLE MUSARK_CONSERVATION.EVENT_ACTOR_ROLE_DATE(
event_id  NUMBER(20)NOT NULL,
actor_id VARCHAR2(36) NOT NULL,
role_id INTEGER NOT NULL,
actor_role_date DATE,
PRIMARY KEY (event_id,actor_id,role_id)
--CONSTRAINT FK_ARD_EVENTID FOREIGN KEY (event_id) references MUSARK_CONSERVERING.EVENT(event_id),
--CONSTRAINT FK_ARD_ROLEID FOREIGN KEY (role_id) references MUSARK_CONSERVERING.ROLE(role_id)
);

CREATE SEQUENCE MUSARK_CONSERVATION.treatment_material_seq
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;

CREATE TABLE MUSARK_CONSERVATION.TREATMENT_MATERIAL(
material_id INTEGER DEFAULT MUSARK_CONSERVATION.treatment_material_seq.nextval,
no_material VARCHAR2(100),
en_material VARCHAR2(100),
PRIMARY KEY (material_id)
);

CREATE SEQUENCE MUSARK_CONSERVATION.treatment_keyword_seq
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;

CREATE TABLE MUSARK_CONSERVATION.TREATMENT_KEYWORD(
keyword_id INTEGER DEFAULT MUSARK_CONSERVATION.treatment_keyword_seq.nextval,
no_keyword VARCHAR2(100),
en_keyword VARCHAR2(100),
PRIMARY KEY (keyword_id)
);


CREATE TABLE MUSARK_CONSERVATION.CONDITION_CODE(
condition_code INTEGER NOT NULL,
no_condition VARCHAR2(100),
en_condition VARCHAR2(100),
PRIMARY KEY (condition_code)
);


CREATE TABLE MUSARK_CONSERVATION.EVENT_DOCUMENT(
event_id  NUMBER(20) NOT NULL,
file_id varchar2(36) NOT NULL,
PRIMARY KEY (event_id,file_id)
);


CREATE SEQUENCE MUSARK_CONSERVATION.MATERIAL_SEQ
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE
NOCACHE;

CREATE TABLE MUSARK_CONSERVATION.MATERIAL_COLLECTION
(
 MATERIAL_ID   INTEGER  DEFAULT MUSARK_CONSERVATION.MATERIAL_SEQ.NEXTVAL,
 collection_id INTEGER,
 old_matrid    INTEGER,
 PRIMARY KEY(MATERIAL_ID)
);

CREATE TABLE MUSARK_CONSERVATION.MATERIAL_ARCHAEOLOGY
(
 material_id INTEGER NOT NULL,
 no_material VARCHAR2(200),
 en_material VARCHAR2(200),
 hidden INTEGER DEFAULT 0 NOT NULL,
 PRIMARY KEY(MATERIAL_ID),
 FOREIGN KEY (MATERIAL_ID) REFERENCES MUSARK_CONSERVATION.MATERIAL_COLLECTION (MATERIAL_ID)
);

CREATE TABLE MUSARK_CONSERVATION.MATERIAL_NUMISMATIC
(
 material_id INTEGER NOT NULL,
 no_material VARCHAR2(200),
 en_material VARCHAR2(200),
 hidden INTEGER DEFAULT 0 NOT NULL,
 PRIMARY KEY(MATERIAL_ID),
 FOREIGN KEY (MATERIAL_ID) REFERENCES MUSARK_CONSERVATION.MATERIAL_COLLECTION (MATERIAL_ID)
);

CREATE TABLE MUSARK_CONSERVATION.MATERIAL_ETHNOGRAPHY
(
 material_id         INTEGER NOT NULL,
 no_material         VARCHAR2(200),
 no_material_type    VARCHAR2(200),
 no_material_element VARCHAR2(200),
 en_material         VARCHAR2(200),
 en_material_type    VARCHAR2(200),
 en_material_element VARCHAR2(200),
 fr_material         VARCHAR2(200),
 fr_material_type    VARCHAR2(200),
 superior_level      VARCHAR2(200),
 hidden INTEGER DEFAULT 0 NOT NULL,
 PRIMARY KEY(MATERIAL_ID),
 FOREIGN KEY (MATERIAL_ID) REFERENCES MUSARK_CONSERVATION.MATERIAL_COLLECTION (MATERIAL_ID)
);


CREATE TABLE MUSARK_CONSERVATION.EA_EVENT_MATERIAL
(
 event_id       NUMBER(20) NOT NULL,
 material_id    INTEGER NOT NULL,
 material_extra VARCHAR2(255),
 sorting        INTEGER,
 PRIMARY KEY(EVENT_ID,MATERIAL_ID),
 FOREIGN KEY (MATERIAL_ID) REFERENCES MUSARK_CONSERVATION.MATERIAL_COLLECTION (MATERIAL_ID),
 FOREIGN KEY (EVENT_ID) REFERENCES MUSARK_CONSERVATION.EVENT(EVENT_ID)
);

-- Grant all rights on tables to musit user
BEGIN
  FOR x IN (SELECT owner || '.' || table_name ownertab
            FROM all_tables
            WHERE owner LIKE 'MUSARK_%' OR owner = 'MUSIT_MAPPING')
  LOOP
    EXECUTE IMMEDIATE 'GRANT ALL ON ' || x.ownertab || ' TO MUSIT';
  END LOOP;
END;
