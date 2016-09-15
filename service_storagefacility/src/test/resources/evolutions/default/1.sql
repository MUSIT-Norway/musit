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
  storage_unit_id NUMBER(20) NOT NULL GENERATED ALWAYS AS IDENTITY,
  storage_unit_name VARCHAR(512),
  area NUMBER,
  area_to NUMBER,
  is_storage_unit VARCHAR(1) DEFAULT '1',
  is_part_of NUMBER(20),
  height NUMBER,
  height_to NUMBER,
  is_deleted INTEGER DEFAULT 0 NOT NULL,
  storage_type VARCHAR(100) DEFAULT 'StorageUnit',
  group_read VARCHAR(4000),
  group_write VARCHAR(4000),
  PRIMARY KEY (storage_unit_id)
);

CREATE TABLE MUSARK_STORAGE.ROOM (
  storage_unit_id NUMBER(20) NOT NULL,
  sikring_skallsikring INTEGER,
  sikring_tyverisikring INTEGER,
  sikring_brannsikring INTEGER,
  sikring_vannskaderisiko INTEGER,
  sikring_rutine_og_beredskap INTEGER,
  bevar_luftfukt_og_temp INTEGER,
  bevar_lysforhold INTEGER,
  bevar_prevant_kons INTEGER,
  PRIMARY KEY (storage_unit_id),
  FOREIGN KEY (storage_unit_id) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id)
);

CREATE TABLE MUSARK_STORAGE.BUILDING (
  storage_unit_id NUMBER(20) NOT NULL,
  postal_address VARCHAR(512),
  PRIMARY KEY (storage_unit_id),
  FOREIGN KEY (storage_unit_id) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id)
);

CREATE TABLE MUSARK_STORAGE.STORAGE_UNIT_LINK (
  link_id NUMBER(20) NOT NULL,
  storage_unit_id NUMBER(20) NOT NULL,
  link VARCHAR(255) NOT NULL,
  relation VARCHAR(100) NOT NULL,
  PRIMARY KEY (link_id),
  FOREIGN KEY (storage_unit_id) REFERENCES MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id)
);

-- ===========================================================================
-- Event specific tables.
-- ===========================================================================

CREATE TABLE MUSARK_STORAGE.EVENT_TYPE (
  id INTEGER NOT NULL ,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  PRIMARY KEY (ID)
);

-- The main table for storing events in the StorageFacility service.
CREATE TABLE MUSARK_STORAGE.EVENT (
  id NUMBER(20) NOT NULL GENERATED ALWAYS AS IDENTITY,
  event_type_id INTEGER NOT NULL, -- Move to separate table if we want to allow multiple instantiations
  note VARCHAR2(4000),

  event_date DATE NOT NULL, -- When the event happened

  registered_by VARCHAR2(100) NOT NULL,
  registered_date TIMESTAMP NOT NULL, -- When the event was received by the system

  value_long LONG, -- Custom value, events can choose to store some event-specific value here.
  value_string CLOB, -- Custom value, events can choose to store some event-specific value here.
  value_float FLOAT, -- Custom value, events can choose to store some event-specific value here.

  part_of LONG,
  PRIMARY KEY (id),
  FOREIGN KEY (event_type_id) REFERENCES MUSARK_STORAGE.EVENT_TYPE(id),
  FOREIGN KEY (part_of) REFERENCES MUSARK_STORAGE.EVENT(id)
);

-- ???
CREATE TABLE MUSARK_STORAGE.EVENT_RELATION_EVENT (
  from_event_id NUMBER(20) NOT NULL,
  relation_id INTEGER NOT NULL,
  to_event_id NUMBER(20) NOT NULL,
  FOREIGN KEY (from_event_id) REFERENCES MUSARK_STORAGE.EVENT(id),
  FOREIGN KEY (to_event_id) REFERENCES MUSARK_STORAGE.EVENT(id)
);

CREATE TABLE MUSARK_STORAGE.ACTOR_ROLE (
  id INTEGER NOT NULL,
  name VARCHAR2(200) NOT NULL,
  description VARCHAR2(200),
  PRIMARY KEY (ID)
);

CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_ACTOR (
  event_id NUMBER(20) NOT NULL,
  role_id INTEGER NOT NULL,
  actor_id INTEGER NOT NULL, -- reference by Id to the ActorService
  PRIMARY KEY (event_id, role_id, actor_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT(id),
  FOREIGN KEY (role_id) REFERENCES MUSARK_STORAGE.ACTOR_ROLE(id)
);

CREATE TABLE MUSARK_STORAGE.OBJECT_ROLE (
  id INTEGER NOT NULL,
  name VARCHAR2(200) NOT NULL,
  description VARCHAR2(200),
  PRIMARY KEY (id)
);

CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT (
  object_id NUMBER(20) NOT NULL,
  latest_move_id NUMBER(20) ,
  current_location_id NUMBER(20), -- maybe for later use
  FOREIGN KEY (latest_move_id) REFERENCES MUSARK_STORAGE.EVENT(id)
  --FOREIGN KEY (current_location_id) REFERENCES MUSARK_STORAGE.storageAdminNodehvatever(ID)
);

CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_OBJECT (
  event_id NUMBER(20) NOT NULL,
  role_id INTEGER NOT NULL,
  object_id NUMBER(20) NOT NULL,
  PRIMARY KEY (event_id, role_id, object_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT(id),
  FOREIGN KEY (role_id) REFERENCES MUSARK_STORAGE.OBJECT_ROLE(id),
  FOREIGN KEY (object_id) REFERENCES MUSARK_STORAGE.LOCAL_OBJECT(object_id)
);

CREATE TABLE MUSARK_STORAGE.PLACE_ROLE (
  id INTEGER NOT NULL,
  name VARCHAR2(200) NOT NULL,
  description VARCHAR2(200),
  PRIMARY KEY (id)
);

-- This is the generic event-to-place relation, the place of where an event
-- happened, the place of where something was moved to etc.
CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_PLACE (
  event_id NUMBER(20) NOT NULL,
  role_id INTEGER NOT NULL,
  place_id  NUMBER(20) NOT NULL,
  PRIMARY KEY (event_id, role_id, place_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT(id),
  FOREIGN KEY (role_id) REFERENCES MUSARK_STORAGE.PLACE_ROLE(id)--,
  --FOREIGN KEY (PLACE_ID) REFERENCES MUSARK_STORAGE.PLACE(PLACE_ID)
);

-- For some event types, the "objects" really are places. Read it as
-- Event_Role_PlaceAsObject. This situation is stored in this table. We could
-- have used the EVENT_ROLE_OBJECT, but then we would loose foreign keys and
-- would have needed to tag which is which.
CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_PLACE_AS_OBJECT (
  event_id NUMBER(20) NOT NULL,
  role_id INTEGER NOT NULL,
  place_id  NUMBER(20) NOT NULL,
  PRIMARY KEY (event_id, role_id, place_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT(id),
  FOREIGN KEY (role_id) REFERENCES MUSARK_STORAGE.OBJECT_ROLE(id)--,
  --FOREIGN KEY (PLACE_ID) REFERENCES MUSARK_STORAGE.PLACE(PLACE_ID)
);

-- ===========================================================================
-- Tables for Events with additional attributes. These do not fit into the
-- three columns set aside for the custom attributes in the EVENT table.
-- ===========================================================================

-- Contains data for Events that have a FROM -> TO structure.
CREATE TABLE MUSARK_STORAGE.OBSERVATION_FROM_TO (
  id NUMBER(20) NOT NULL,
  value_from NUMBER,
  value_to NUMBER,
  PRIMARY KEY (id),
  FOREIGN KEY (id) REFERENCES MUSARK_STORAGE.EVENT(id)
);

-- Contains extra data for storing environment requirement.
CREATE TABLE MUSARK_STORAGE.E_ENVIRONMENT_REQUIREMENT (
  id NUMBER(20) NOT NULL,
  temperature NUMBER,
  temp_interval NUMBER,
  air_humidity NUMBER,
  air_hum_interval NUMBER,
  hypoxic_air NUMBER,
  hyp_air_interval NUMBER,
  cleaning VARCHAR2(250),
  light VARCHAR2(250),
  PRIMARY KEY (id),
  FOREIGN KEY (id) REFERENCES MUSARK_STORAGE.EVENT(id)
);

-- Contains extra data for storing info about a pest lifecycle. It is used by
-- the ObservationPest event which can have many of these.
CREATE TABLE MUSARK_STORAGE.OBSERVATION_PEST_LIFECYCLE (
  event_id NUMBER(20) NOT NULL,
  stage VARCHAR2(250),
  num INTEGER, -- number is a keyword in Oracle and should be quoted
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT(id)
);

-- NOTE: This table is currently not used.
CREATE TABLE MUSARK_STORAGE.URI_LINKS (
  id NUMBER(20) NOT NULL GENERATED ALWAYS AS IDENTITY,
  local_table_id NUMBER(20) NOT NULL,
  rel VARCHAR(255) NOT NULL,
  href VARCHAR(2000) NOT NULL,
  PRIMARY KEY (id)
);

-- ===========================================================================
-- Inserting dummy data
-- ===========================================================================

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (1, 'MoveObject');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (2, 'MovePlace');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (3, 'EnvRequirement');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (4, 'Control');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (5, 'Observation');

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (6, 'ControlAlcohol');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (7, 'ControlCleaning');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (8, 'ControlGas');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (9, 'ControlHypoxicAir');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (10, 'ControlLightingCondition');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (11, 'ControlMold');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (12, 'ControlPest');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (13, 'ControlRelativeHumidity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (14, 'ControlTemperature');

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (15, 'ObservationAlcohol');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (16, 'ObservationCleaning');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (17, 'ObservationFireProtection');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (18, 'ObservationGas');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (19, 'ObservationHypoxicAir');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (20, 'ObservationLightingCondition');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (21, 'ObservationMold');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (22, 'ObservationPerimeterSecurity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (23, 'ObservationRelativeHumidity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (24, 'ObservationPest');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (25, 'ObservationTemperature');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (26, 'ObservationTheftProtection');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (id,name) VALUES (27, 'ObservationWaterDamageAssessment');

INSERT INTO MUSARK_STORAGE.ACTOR_ROLE (id, name, description)
VALUES (1, 'DoneBy', 'The actor who has executed/done the event');

INSERT INTO MUSARK_STORAGE.OBJECT_ROLE (id, name, description)
VALUES (1, 'DoneWith', 'The object who was done something with in a spesific event');

INSERT INTO MUSARK_STORAGE.PLACE_ROLE (id, name, description)
VALUES (1, 'DoneWith', 'The storagenode who was done something with in a spesific event');


INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,height,area,storage_type) VALUES (1,'KASSE 5',45,45,'storageunit');
INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,height,area,storage_type) VALUES (2,'KASSE 6',1,4,'storageunit');
INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,height,area,storage_type) VALUES (3,'KASSE 7',3,4,'storageunit');
INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,height,area,storage_type) VALUES (7,'KASSE 12',3,4,'storageunit');
INSERT INTO MUSARK_STORAGE.STORAGE_UNIT(storage_unit_id,storage_unit_name,area,storage_type) VALUES (9,'KASSE 12',4,'storageunit');

# --- !Downs
