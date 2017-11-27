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

ALTER USER musark_auth QUOTA UNLIMITED ON USERS;
ALTER USER musark_core QUOTA UNLIMITED ON USERS;
ALTER USER musark_storage QUOTA UNLIMITED ON USERS;
ALTER USER musark_actor QUOTA UNLIMITED ON USERS;
ALTER USER musark_analysis QUOTA UNLIMITED ON USERS;
ALTER USER musark_loan QUOTA UNLIMITED ON USERS;
ALTER USER musit_mapping QUOTA UNLIMITED ON USERS;
ALTER USER musark_conservation QUOTA UNLIMITED ON USERS;

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
  type_id         VARCHAR2(36)             NOT NULL,
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
-- auth groups for document module
INSERT INTO musark_auth.auth_group VALUES ('73367f36-0e9a-4ef9-8164-886bd582adc9', 'AmDmRead'   , 3, 10, 1 , 'Read access to document module for AM');
INSERT INTO musark_auth.auth_group VALUES ('58f4c712-a66b-460e-a70f-333d465c4349', 'AmDmWrite'  , 3, 20, 1 , 'Write access to document module for AM');
INSERT INTO musark_auth.auth_group VALUES ('18d1bf6f-afc6-4d62-a55a-7736d86d5abf', 'AmDmAdmin'  , 3, 30, 1 , 'Admin access to document module for AM');
INSERT INTO musark_auth.auth_group VALUES ('52185e0d-8876-4342-9329-084f3bb01216', 'UmDmRead'   , 3, 10, 2 , 'Read access to document module for UM');
INSERT INTO musark_auth.auth_group VALUES ('9c0b963f-4ea1-4814-9369-a0f71816873b', 'UmDmWrite'  , 3, 20, 2 , 'Write access to document module for UM');
INSERT INTO musark_auth.auth_group VALUES ('0ce4e5c3-53ef-4bcc-a388-1a5710967966', 'UmDmAdmin'  , 3, 30, 2 , 'Admin access to document module for UM');
INSERT INTO musark_auth.auth_group VALUES ('a9fa1ab7-ab62-44f3-aa3b-91470e2123f1', 'KhmDmRead'  , 3, 10, 3 , 'Read access to document module for KHM');
INSERT INTO musark_auth.auth_group VALUES ('918f161e-2d1d-431d-b903-8d315f70165a', 'KhmDmWrite' , 3, 20, 3 , 'Write access to document module for KHM');
INSERT INTO musark_auth.auth_group VALUES ('d06d9c74-85ce-4b18-b15c-6395cbc5a874', 'KhmDmAdmin' , 3, 30, 3 , 'Admin access to document module for KHM');
INSERT INTO musark_auth.auth_group VALUES ('22c93f16-fae2-4b93-a574-5b40fbeea4f3', 'NhmDmRead'  , 3, 10, 4 , 'Read access to document module for NHM');
INSERT INTO musark_auth.auth_group VALUES ('d4ea5037-a8d0-45f6-b15e-534f702f507f', 'NhmDmWrite' , 3, 20, 4 , 'Write access to document module for NHM');
INSERT INTO musark_auth.auth_group VALUES ('716adceb-6b54-4f8e-887d-f7612c347614', 'NhmDmAdmin' , 3, 30, 4 , 'Admin access to document module for NHM');
INSERT INTO musark_auth.auth_group VALUES ('f8a2a480-afdf-426e-a9b7-7d4fc2944a3f', 'VmDmRead'   , 3, 10, 5 , 'Read access to document module for VM');
INSERT INTO musark_auth.auth_group VALUES ('c166094d-0d54-47f0-a8e3-91348acfe329', 'VmDmWrite'  , 3, 20, 5 , 'Write access to document module for VM');
INSERT INTO musark_auth.auth_group VALUES ('94d2f793-cc2c-4573-b190-b55a06d25836', 'VmDmAdmin'  , 3, 30, 5 , 'Admin access to document module for VM');
INSERT INTO musark_auth.auth_group VALUES ('26ae0cfe-5bb3-4ede-8025-8d3b1e2b0ae3', 'TmuDmRead'  , 3, 10, 6 , 'Read access to document module for TMU');
INSERT INTO musark_auth.auth_group VALUES ('59f07127-da64-4106-b716-3d88a428b15b', 'TmuDmWrite' , 3, 20, 6 , 'Write access to document module for TMU');
INSERT INTO musark_auth.auth_group VALUES ('68381fe7-b6ab-455a-b57e-1e959ecdef00', 'TmuDmAdmin' , 3, 30, 6 , 'Admin access to document module for TMU');
INSERT INTO musark_auth.auth_group VALUES ('62dfb712-7524-4917-86c2-4b7cd7440dd8', 'KmnDmRead'  , 3, 10, 7 , 'Read access to document module for KMN');
INSERT INTO musark_auth.auth_group VALUES ('093b1751-0c18-4426-89ec-1f22baf6f47a', 'KmnDmWrite' , 3, 20, 7 , 'Write access to document module for KMN');
INSERT INTO musark_auth.auth_group VALUES ('bd8d1872-da0d-45aa-af7e-37067b3cac2b', 'KmnDmAdmin' , 3, 30, 7 , 'Admin access to document module for KMN');
INSERT INTO musark_auth.auth_group VALUES ('a5dba794-e2a3-4dab-b5a4-71400c107dda', 'TestDmRead' , 3, 10, 99, 'Read access to document module for TEST');
INSERT INTO musark_auth.auth_group VALUES ('cc823554-b34d-45d3-8e16-03d4a79014e3', 'TestDmWrite', 3, 20, 99, 'Write access to document module for TEST');
INSERT INTO musark_auth.auth_group VALUES ('38907655-4908-4ab6-b4e9-fbee63fb2d34', 'TestDmAdmin', 3, 30, 99, 'Admin access to document module for TEST');


CREATE TABLE MUSARK_CONSERVATION.EVENT (
  event_id        NUMBER(20) DEFAULT MUSARK_CONSERVATION.event_eventid_seq.nextval,
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
  case_number    VARCHAR2(1000),
  event_json      CLOB,
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


-- Grant all rights on tables to musit user
BEGIN
  FOR x IN (SELECT owner || '.' || table_name ownertab
            FROM all_tables
            WHERE owner LIKE 'MUSARK_%' OR owner = 'MUSIT_MAPPING')
  LOOP
    EXECUTE IMMEDIATE 'GRANT ALL ON ' || x.ownertab || ' TO MUSIT';
  END LOOP;
END;
