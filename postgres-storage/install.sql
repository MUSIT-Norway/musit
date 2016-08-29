CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

CREATE SEQUENCE MUSARK_STORAGE.STORAGE_UNIT_seq;

CREATE TABLE MUSARK_STORAGE.STORAGE_UNIT(
  storage_unit_id   BIGINT NOT NULL  DEFAULT NEXTVAL ('MUSARK_STORAGE.STORAGE_UNIT_seq'),
  storage_unit_name VARCHAR(512),
  area              INTEGER,
  area_to           INTEGER,
  is_storage_unit   VARCHAR(1) DEFAULT '1',
  is_part_of        INTEGER,
  height            INTEGER,
  height_to         INTEGER,
  is_deleted        integer not null default 0,
  storage_type      varchar(100) default 'StorageUnit',
  group_read        varchar(4000),
  group_write       varchar(4000),
  primary key (storage_unit_id)
);

CREATE TABLE MUSARK_STORAGE.ROOM(
  storage_unit_id             BIGINT not null,
  sikring_skallsikring        integer,
  sikring_tyverisikring       integer,
  sikring_brannsikring        integer,
  sikring_vannskaderisiko     integer,
  sikring_rutine_og_beredskap integer,
  bevar_luftfukt_og_temp      integer,
  bevar_lysforhold            integer,
  bevar_prevant_kons          integer,
  PRIMARY KEY (STORAGE_UNIT_ID),
  FOREIGN KEY (STORAGE_UNIT_ID) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(STORAGE_UNIT_ID)
);

CREATE TABLE MUSARK_STORAGE.BUILDING(
  storage_unit_id INTEGER not null ,
  postal_address  VARCHAR(512),
  PRIMARY KEY (STORAGE_UNIT_ID),
  FOREIGN KEY (STORAGE_UNIT_ID) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(STORAGE_UNIT_ID)
);

CREATE TABLE MUSARK_STORAGE.STORAGE_UNIT_LINK(
  link_id         BIGINT not null,
  storage_unit_id BIGINT not null,
  link            VARCHAR(255) not null,
  relation        VARCHAR(100) not null,
  PRIMARY KEY (link_id),
  FOREIGN KEY (STORAGE_UNIT_ID) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(STORAGE_UNIT_ID)
);

CREATE SCHEMA IF NOT EXISTS MUSARK_EVENT;


CREATE TABLE MUSARK_EVENT.EVENT_TYPE (
  ID INTEGER NOT NULL ,
  Name VARCHAR(100) NOT NULL,
  Description VARCHAR(255),
  PRIMARY KEY (ID)
);


CREATE SEQUENCE MUSARK_EVENT.EVENT_seq;

CREATE TABLE MUSARK_EVENT.EVENT (
  ID BIGINT NOT NULL DEFAULT NEXTVAL ('MUSARK_EVENT.EVENT_seq'),
  EVENT_TYPE_ID integer not null, -- Move to separate table if we want to allow multiple instantiations
  NOTE VARCHAR(4000),

  EVENT_DATE timestamp(0), -- When the event happened

  REGISTERED_BY VARCHAR(100),
  REGISTERED_DATE timestamp, -- could probably equivalently use datetime.

  VALUE_LONG text, -- Custom value, events can choose to store some event-specific value here.
  VALUE_String text, -- Custom value, events can choose to store some event-specific value here.
  VALUE_FLOAT double precision, -- Custom value, events can choose to store some event-specific value here.

  PART_OF text,
  PRIMARY KEY (ID),
  FOREIGN KEY (EVENT_TYPE_ID) REFERENCES MUSARK_EVENT.EVENT_TYPE(ID),
  FOREIGN KEY (PART_OF) REFERENCES MUSARK_EVENT.EVENT(ID)
);


CREATE TABLE MUSARK_EVENT.EVENT_RELATION_EVENT (
  FROM_EVENT_ID BIGINT NOT NULL,
  RELATION_ID integer not null,
  TO_EVENT_ID BIGINT NOT NULL,
  FOREIGN KEY (FROM_EVENT_ID) REFERENCES MUSARK_EVENT.EVENT(ID),
  FOREIGN KEY (TO_EVENT_ID) REFERENCES MUSARK_EVENT.EVENT(ID)
);

CREATE TABLE MUSARK_EVENT.ACTOR_ROLE (
  ID Integer NOT NULL,
  NAME varchar(200) NOT NULL,
  DESCRIPTION varchar(200),
  PRIMARY KEY (ID)
);

insert into MUSARK_EVENT.ACTOR_ROLE(ID, NAME, DESCRIPTION) VALUES (1, 'DoneBy', 'The actor who has executed/done the event');

CREATE TABLE MUSARK_EVENT.EVENT_ROLE_ACTOR (
  EVENT_ID BIGINT NOT NULL,
  ROLE_ID Integer NOT NULL,
  ACTOR_ID integer NOT NULL,
  PRIMARY KEY (EVENT_ID, ROLE_ID, ACTOR_ID),
  FOREIGN KEY (EVENT_ID) REFERENCES MUSARK_EVENT.EVENT(ID),
  FOREIGN KEY (ROLE_ID) REFERENCES MUSARK_EVENT.ACTOR_ROLE(ID)
  --Actor_ID is in another microservice so no foreign key allowed... :(
);


CREATE TABLE MUSARK_EVENT.OBSERVATION_FROM_TO (
  ID BIGINT NOT NULL,
  VALUE_FROM DOUBLE PRECISION,
  VALUE_TO DOUBLE PRECISION,
  PRIMARY KEY (ID),
  FOREIGN KEY (ID) REFERENCES MUSARK_EVENT.EVENT(ID)
);

CREATE TABLE MUSARK_EVENT.E_ENVIRONMENT_REQUIREMENT
(
  id         BIGINT NOT NULL,
  temperature             DOUBLE PRECISION,
  temp_interval    DOUBLE PRECISION,
  air_humidity     DOUBLE PRECISION,
  air_hum_interval DOUBLE PRECISION,
  hypoxic_air      DOUBLE PRECISION,
  hyp_air_interval DOUBLE PRECISION,
  cleaning         VARCHAR(250),
  light            VARCHAR(250),
  PRIMARY KEY (ID),
  FOREIGN KEY (ID) REFERENCES MUSARK_EVENT.EVENT(ID)
);


CREATE TABLE MUSARK_EVENT.OBSERVATION_PEST_LIFECYCLE
(
  event_id         BIGINT NOT NULL,
  stage       VARCHAR(250),
  number             integer,
  FOREIGN KEY (event_id) REFERENCES MUSARK_EVENT.EVENT(ID)
);

CREATE SEQUENCE URI_LINKS_seq;

CREATE TABLE URI_LINKS (
  ID bigint NOT NULL DEFAULT NEXTVAL ('URI_LINKS_seq'),
  LOCAL_TABLE_ID bigint NOT NULL,
  REL varchar(255) NOT NULL,
  HREF varchar(2000) NOT NULL,
  PRIMARY KEY (ID)
);

insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (1, 'Move');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (2, 'EnvRequirement');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (3, 'Control');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (4, 'Observation');

insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (5, 'ControlAlcohol');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (6, 'ControlCleaning');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (7, 'ControlGas');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (8, 'ControlHypoxicAir');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (9, 'ControlLightingCondition');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (10, 'ControlMold');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (11, 'ControlPest');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (12, 'ControlRelativeHumidity');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (13, 'ControlTemperature');

insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (14, 'ObservationAlcohol');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (15, 'ObservationCleaning');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (16, 'ObservationFireProtection');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (17, 'ObservationGas');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (18, 'ObservationHypoxicAir');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (19, 'ObservationLightingCondition');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (20, 'ObservationMold');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (21, 'ObservationPerimeterSecurity');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (22, 'ObservationRelativeHumidity');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (23, 'ObservationPest');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (24, 'ObservationTemperature');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (25, 'ObservationTheftProtection');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (26, 'ObservationWaterDamageAssessment');


CREATE SCHEMA IF NOT EXISTS MUSIT_MAPPING;

CREATE SEQUENCE MUSIT_MAPPING.VIEW_ACTOR_seq;

CREATE TABLE MUSIT_MAPPING.VIEW_ACTOR (
  NY_ID BIGINT NOT NULL DEFAULT NEXTVAL ('MUSIT_MAPPING.VIEW_ACTOR_seq'),
  ACTORNAME VARCHAR(512),
  PRIMARY KEY (NY_ID)
);

CREATE SCHEMA IF NOT EXISTS MUSARK_ACTOR;

CREATE SEQUENCE MUSARK_ACTOR.PERSON_seq;

CREATE TABLE MUSARK_ACTOR.PERSON (
  ID BIGINT NOT NULL DEFAULT NEXTVAL ('MUSARK_ACTOR.PERSON_seq'),
  FN VARCHAR(255),
  TITLE VARCHAR(255),
  ROLE VARCHAR(255),
  TEL VARCHAR(20),
  EMAiL VARCHAR(255),
  PRIMARY KEY (ID)
);

CREATE SEQUENCE MUSARK_ACTOR.ORGANIZATION_seq;

CREATE TABLE MUSARK_ACTOR.ORGANIZATION (
  ID BIGINT NOT NULL DEFAULT NEXTVAL ('MUSARK_ACTOR.ORGANIZATION_seq'),
  FN VARCHAR(255) NOT NULL,
  NICKNAME VARCHAR(255),
  TEL VARCHAR(20),
  WEB VARCHAR(255),
  PRIMARY KEY (ID)
);

CREATE SEQUENCE MUSARK_ACTOR.ORGANIZATION_ADDRESS_seq;

CREATE TABLE MUSARK_ACTOR.ORGANIZATION_ADDRESS (
  ID BIGINT NOT NULL DEFAULT NEXTVAL ('MUSARK_ACTOR.ORGANIZATION_ADDRESS_seq'),
  ORGANIZATION_ID BIGINT NOT NULL,
  TYPE VARCHAR(20),
  STREET_ADDRESS VARCHAR(20),
  LOCALITY VARCHAR(255),
  POSTAL_CODE VARCHAR(12),
  COUNTRY_NAME VARCHAR(255),
  LATITUDE DOUBLE PRECISION,
  LONGITUDE DOUBLE PRECISION,
  PRIMARY KEY (ID)
);
ALTER TABLE MUSARK_ACTOR.ORGANIZATION_ADDRESS ADD FOREIGN KEY (ORGANIZATION_ID) REFERENCES MUSARK_ACTOR.ORGANIZATION(ID);

insert into MUSIT_MAPPING.VIEW_ACTOR (actorname) values ('And, Arne1');
insert into MUSIT_MAPPING.VIEW_ACTOR (actorname) values ('Kanin, Kalle1');
insert into MUSARK_ACTOR.PERSON (FN, TITLE, ROLE, TEL, EMAIL) values ('Klaus Myrseth', 'Løsnings arkitekt', 'System arkitekt', '93297177', 'klaus.myrseth@usit.uio.no');
insert into MUSARK_ACTOR.ORGANIZATION (ID, FN, NICKNAME, TEL, WEB) values (1, 'Kulturhistorisk museum - Universitetet i Oslo', 'KHM', '22 85 19 00', 'www.khm.uio.no');
insert into MUSARK_ACTOR.ORGANIZATION_ADDRESS (ORGANIZATION_ID, TYPE, STREET_ADDRESS, LOCALITY, POSTAL_CODE, COUNTRY_NAME, LATITUDE, LONGITUDE) values (1, 'WORK', 'Fredriks gate 2', 'OSLO', '0255', 'NORWAY', 0.0, 0.0);

