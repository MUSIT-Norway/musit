-- noinspection SqlDialectInspectionForFile
#
#  MUSIT is a museum database to archive natural and cultural history data.
#  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License,
#  or any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License along
#  with this program; if not, write to the Free Software Foundation, Inc.,
#  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
#
# STORAGE FACILITY schema

# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

-- ===========================================================================
-- Tables for storage nodes
-- ===========================================================================

CREATE TABLE MUSARK_STORAGE.STORAGE_UNIT (
  storage_unit_id BIGINT NOT NULL AUTO_INCREMENT,
  storage_unit_name VARCHAR(512),
  area INTEGER,
  area_to INTEGER,
  is_storage_unit VARCHAR(1) DEFAULT '1',
  is_part_of INTEGER,
  height INTEGER,
  height_to INTEGER,
  is_deleted INTEGER NOT NULL DEFAULT 0,
  storage_type VARCHAR(100) DEFAULT 'StorageUnit',
  group_read VARCHAR(4000),
  group_write VARCHAR(4000),
  PRIMARY KEY (storage_unit_id)
);

CREATE TABLE MUSARK_STORAGE.ROOM (
  storage_unit_id BIGINT NOT NULL,
  sikring_skallsikring INTEGER,
  sikring_tyverisikring INTEGER,
  sikring_brannsikring INTEGER,
  sikring_vannskaderisiko INTEGER,
  sikring_rutine_og_beredskap INTEGER,
  bevar_luftfukt_og_temp INTEGER,
  bevar_lysforhold INTEGER,
  bevar_prevant_kons INTEGER,
  PRIMARY KEY (STORAGE_UNIT_ID),
  FOREIGN KEY (STORAGE_UNIT_ID) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id)
);

CREATE TABLE MUSARK_STORAGE.BUILDING (
  storage_unit_id BIGINT NOT NULL,
  postal_address VARCHAR(512),
  PRIMARY KEY (storage_unit_id),
  FOREIGN KEY (storage_unit_id) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id)
);

CREATE TABLE MUSARK_STORAGE.STORAGE_UNIT_LINK (
  link_id BIGINT NOT NULL,
  storage_unit_id BIGINT NOT NULL,
  link VARCHAR(255) NOT NULL,
  relation VARCHAR(100) NOT NULL,
  PRIMARY KEY (link_id),
  FOREIGN KEY (STORAGE_UNIT_ID) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(STORAGE_UNIT_ID)
);

-- ===========================================================================
-- Event specific tables.
-- ===========================================================================

CREATE TABLE MUSARK_STORAGE.EVENT_TYPE (
  ID INTEGER NOT NULL ,
  Name VARCHAR(100) NOT NULL,
  Description VARCHAR(255),
  PRIMARY KEY (ID)
);

-- The main table for storing events in the StorageFacility service.
CREATE TABLE MUSARK_STORAGE.EVENT (
  ID BIGINT(20) NOT NULL AUTO_INCREMENT,
  EVENT_TYPE_ID INTEGER NOT NULL, -- Move to separate table if we want to allow multiple instantiations
  NOTE VARCHAR2(4000),

  EVENT_DATE DATE NOT NULL, -- When the event happened

  REGISTERED_BY VARCHAR2(100) NOT NULL,
  REGISTERED_DATE TIMESTAMP NOT NULL, -- When the event was received by the system

  VALUE_LONG LONG, -- Custom value, events can choose to store some event-specific value here.
  VALUE_String CLOB, -- Custom value, events can choose to store some event-specific value here.
  VALUE_FLOAT FLOAT, -- Custom value, events can choose to store some event-specific value here.

  PART_OF LONG,
  PRIMARY KEY (ID),
  FOREIGN KEY (EVENT_TYPE_ID) REFERENCES MUSARK_STORAGE.EVENT_TYPE(ID),
  FOREIGN KEY (PART_OF) REFERENCES MUSARK_STORAGE.EVENT(ID)
);

-- ???
CREATE TABLE MUSARK_STORAGE.EVENT_RELATION_EVENT (
  FROM_EVENT_ID BIGINT(20) NOT NULL,
  RELATION_ID INTEGER NOT NULL,
  TO_EVENT_ID BIGINT(20) NOT NULL,
  FOREIGN KEY (FROM_EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID),
  FOREIGN KEY (TO_EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID)
);

CREATE TABLE MUSARK_STORAGE.ACTOR_ROLE (
  ID INTEGER NOT NULL,
  NAME VARCHAR2(200) NOT NULL,
  DESCRIPTION VARCHAR2(200),
  PRIMARY KEY (ID)
);

CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_ACTOR (
  EVENT_ID BIGINT(20) NOT NULL,
  ROLE_ID INTEGER NOT NULL,
  ACTOR_ID INTEGER NOT NULL, -- reference by Id to the ActorService
  PRIMARY KEY (EVENT_ID, ROLE_ID, ACTOR_ID),
  FOREIGN KEY (EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID),
  FOREIGN KEY (ROLE_ID) REFERENCES MUSARK_STORAGE.ACTOR_ROLE(ID)
);

CREATE TABLE MUSARK_STORAGE.OBJECT_ROLE (
  ID INTEGER NOT NULL,
  NAME VARCHAR2(200) NOT NULL,
  DESCRIPTION VARCHAR2(200),
  PRIMARY KEY (ID)
);

CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT (
  object_id BIGINT(20) NOT NULL,
  latest_move_id BIGINT(20) ,
  current_location_id INTEGER, -- maybe for later use
  FOREIGN KEY (latest_move_id) REFERENCES MUSARK_STORAGE.EVENT(ID)
  --FOREIGN KEY (current_location_id) REFERENCES MUSARK_STORAGE.storageAdminNodehvatever(ID)
);

CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_OBJECT (
  EVENT_ID BIGINT(20) NOT NULL,
  ROLE_ID INTEGER NOT NULL,
  OBJECT_ID INTEGER NOT NULL,
  PRIMARY KEY (EVENT_ID, ROLE_ID, OBJECT_ID),
  FOREIGN KEY (EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID),
  FOREIGN KEY (ROLE_ID) REFERENCES MUSARK_STORAGE.OBJECT_ROLE(ID),
  FOREIGN KEY (OBJECT_ID) REFERENCES MUSARK_STORAGE.LOCAL_OBJECT(OBJECT_ID)
);

CREATE TABLE MUSARK_STORAGE.PLACE_ROLE (
  ID INTEGER NOT NULL,
  NAME VARCHAR2(200) NOT NULL,
  DESCRIPTION VARCHAR2(200),
  PRIMARY KEY (ID)
);

-- This is the generic event-to-place relation, the place of where an event
-- happened, the place of where something was moved to etc.
CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_PLACE (
  EVENT_ID BIGINT(20) NOT NULL,
  ROLE_ID INTEGER NOT NULL,
  PLACE_ID  INTEGER NOT NULL,
  PRIMARY KEY (EVENT_ID, ROLE_ID, PLACE_ID),
  FOREIGN KEY (EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID),
  FOREIGN KEY (ROLE_ID) REFERENCES MUSARK_STORAGE.PLACE_ROLE(ID)--,
  --FOREIGN KEY (PLACE_ID) REFERENCES MUSARK_STORAGE.PLACE(PLACE_ID)
);

-- For some event types, the "objects" really are places. Read it as
-- Event_Role_PlaceAsObject. This situation is stored in this table. We could
-- have used the EVENT_ROLE_OBJECT, but then we would loose foreign keys and
-- would have needed to tag which is which.
CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_PLACE_AS_OBJECT (
  EVENT_ID BIGINT(20) NOT NULL,
  ROLE_ID INTEGER NOT NULL,
  PLACE_ID  INTEGER NOT NULL,
  PRIMARY KEY (EVENT_ID, ROLE_ID, PLACE_ID),
  FOREIGN KEY (EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID),
  FOREIGN KEY (ROLE_ID) REFERENCES MUSARK_STORAGE.OBJECT_ROLE(ID)--,
  --FOREIGN KEY (PLACE_ID) REFERENCES MUSARK_STORAGE.PLACE(PLACE_ID)
);

-- ===========================================================================
-- Tables for Events with additional attributes. These do not fit into the
-- three columns set aside for the custom attributes in the EVENT table.
-- ===========================================================================

-- Contains data for Events that have a FROM -> TO structure.
CREATE TABLE MUSARK_STORAGE.OBSERVATION_FROM_TO (
  ID BIGINT(20) NOT NULL,
  VALUE_FROM NUMBER,
  VALUE_TO NUMBER,
  PRIMARY KEY (ID),
  FOREIGN KEY (ID) REFERENCES MUSARK_STORAGE.EVENT(ID)
);

-- Contains extra data for storing environment requirement.
CREATE TABLE MUSARK_STORAGE.E_ENVIRONMENT_REQUIREMENT (
  id BIGINT(20) NOT NULL,
  temperature NUMBER,
  temp_interval NUMBER,
  air_humidity NUMBER,
  air_hum_interval NUMBER,
  hypoxic_air NUMBER,
  hyp_air_interval NUMBER,
  cleaning VARCHAR2(250),
  light VARCHAR2(250),
  PRIMARY KEY (ID),
  FOREIGN KEY (ID) REFERENCES MUSARK_STORAGE.EVENT(ID)
);

-- Contains extra data for storing info about a pest lifecycle. It is used by
-- the ObservationPest event which can have many of these.
CREATE TABLE MUSARK_STORAGE.OBSERVATION_PEST_LIFECYCLE (
  event_id BIGINT(20) NOT NULL,
  stage VARCHAR2(250),
  number INTEGER,
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT(ID)
);

-- NOTE: This table is currently not used.
CREATE TABLE MUSARK_STORAGE.URI_LINKS (
  ID bigint(20) NOT NULL AUTO_INCREMENT,
  LOCAL_TABLE_ID BIGINT(20) NOT NULL,
  REL VARCHAR(255) NOT NULL,
  HREF VARCHAR(2000) NOT NULL,
  PRIMARY KEY (ID)
);

-- ===========================================================================
-- Inserting dummy data
-- ===========================================================================

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (1, 'MoveObject');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (2, 'MovePlace');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (3, 'EnvRequirement');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (4, 'Control');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (5, 'Observation');

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (6, 'ControlAlcohol');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (7, 'ControlCleaning');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (8, 'ControlGas');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (9, 'ControlHypoxicAir');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (10, 'ControlLightingCondition');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (11, 'ControlMold');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (12, 'ControlPest');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (13, 'ControlRelativeHumidity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (14, 'ControlTemperature');

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (15, 'ObservationAlcohol');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (16, 'ObservationCleaning');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (17, 'ObservationFireProtection');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (18, 'ObservationGas');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (19, 'ObservationHypoxicAir');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (20, 'ObservationLightingCondition');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (21, 'ObservationMold');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (22, 'ObservationPerimeterSecurity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (23, 'ObservationRelativeHumidity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (24, 'ObservationPest');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (25, 'ObservationTemperature');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (26, 'ObservationTheftProtection');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,Name) VALUES (27, 'ObservationWaterDamageAssessment');

INSERT INTO MUSARK_STORAGE.ACTOR_ROLE (ID, NAME, DESCRIPTION)
VALUES (1, 'DoneBy', 'The actor who has executed/done the event');

INSERT INTO MUSARK_STORAGE.OBJECT_ROLE (ID, NAME, DESCRIPTION)
VALUES (1, 'DoneWith', 'The object who was done something with in a spesific event');

INSERT INTO MUSARK_STORAGE.PLACE_ROLE (ID, NAME, DESCRIPTION)
VALUES (1, 'DoneWith', 'The storagenode who was done something with in a spesific event');
--
--
-- INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,height,area,storage_type) VALUES (1,'KASSE 5',45,45,'storageunit');
-- INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,height,area,storage_type) VALUES (2,'KASSE 6',1,4,'storageunit');
-- INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,height,area,storage_type) VALUES (3,'KASSE 7',3,4,'storageunit');
-- INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,height,area,storage_type) VALUES (7,'KASSE 12',3,4,'storageunit');
-- INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,area,storage_type) VALUES (9,'KASSE 12',4,'storageunit');

# --- !Downs
