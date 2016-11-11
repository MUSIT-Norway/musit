# --STORAGE FACILITY schema

# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

-- ===========================================================================
-- Tables for storage nodes
-- ===========================================================================

CREATE TABLE MUSARK_STORAGE.STORAGE_NODE (
  storage_node_id   NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  storage_node_name VARCHAR(512),
  area              NUMBER,
  area_to           NUMBER,
  is_storage_unit   VARCHAR(1)        DEFAULT '1',
  is_part_of        NUMBER(20),
  height            NUMBER,
  height_to         NUMBER,
  node_path         VARCHAR(1000)     NOT NULL,
  is_deleted        INTEGER DEFAULT 0 NOT NULL,
  storage_type      VARCHAR(100)      DEFAULT 'StorageUnit',
  group_read        VARCHAR(4000),
  group_write       VARCHAR(4000),
  museum_id         INTEGER           NOT NULL,
  updated_by        VARCHAR2(36)      NOT NULL,
  updated_date      TIMESTAMP         NOT NULL, -- When the change was received by the system
  --   latest_move_id NUMBER(20),
  --   latest_envreq_id NUMBER(20),
  PRIMARY KEY (storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.ROOM (
  storage_node_id               NUMBER(20) NOT NULL,
  perimeter_security            INTEGER, -- DEFAULT 0 NOT NULL,
  theft_protection              INTEGER, -- DEFAULT 1 NOT NULL,
  fire_protection               INTEGER, -- DEFAULT 0 NOT NULL,
  water_damage_assessment       INTEGER, -- DEFAULT 0 NOT NULL,
  routines_and_contingency_plan INTEGER, -- DEFAULT 0 NOT NULL,
  relative_humidity             INTEGER, -- DEFAULT 0 NOT NULL,
  temperature_assessment        INTEGER, -- DEFAULT 0 NOT NULL,
  lighting_condition            INTEGER, -- DEFAULT 0 NOT NULL,
  preventive_conservation       INTEGER, -- DEFAULT 0 NOT NULL,
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
  PRIMARY KEY (storage_node_id),
  FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE (storage_node_id)
);

-- ===========================================================================
-- Event specific tables.
-- ===========================================================================

CREATE TABLE MUSARK_STORAGE.ROLE (
  role_id INTEGER GENERATED BY DEFAULT AS IDENTITY,
  name    VARCHAR2(100) NOT NULL,
  type    VARCHAR2(100),
  PRIMARY KEY (role_id)
);

CREATE TABLE MUSARK_STORAGE.EVENT_TYPE (
  event_type_id INTEGER GENERATED BY DEFAULT AS IDENTITY,
  name          VARCHAR(100) NOT NULL,
  PRIMARY KEY (event_type_ID)
);

-- The main table for storing events in the StorageFacility service.
CREATE TABLE MUSARK_STORAGE.EVENT (
  event_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  event_type_id   INTEGER       NOT NULL, -- Move to separate table if we want to allow multiple instantiations
  note            VARCHAR2(500),
  event_date      DATE          NOT NULL, -- When the event happened. Should be nullable.
  registered_by   VARCHAR2(36)  NOT NULL,
  registered_date TIMESTAMP     NOT NULL, -- When the event was received by the system
  value_long      NUMBER(20), -- Custom value, events can choose to store some event-specific value here. NOTE: 20 is probably waaaaaay to much.
  value_string    VARCHAR2(250), -- Custom value, events can choose to store some event-specific value here.
  value_float     FLOAT, -- Custom value, events can choose to store some event-specific value here.
  part_of         NUMBER(20), -- 1 to 1 with a top-level event
  PRIMARY KEY (event_id),
  FOREIGN KEY (event_type_id) REFERENCES MUSARK_STORAGE.EVENT_TYPE (event_type_id),
  FOREIGN KEY (part_of) REFERENCES MUSARK_STORAGE.EVENT (event_id)
);

-- ???
CREATE TABLE MUSARK_STORAGE.EVENT_RELATION_EVENT (
  from_event_id NUMBER(20) NOT NULL,
  relation_id   INTEGER    NOT NULL,
  to_event_id   NUMBER(20) NOT NULL,
  FOREIGN KEY (from_event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id),
  FOREIGN KEY (to_event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id)
);

CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT (
  object_id           NUMBER(20) NOT NULL,
  latest_move_id      NUMBER(20) NOT NULL,
  current_location_id NUMBER(20) NOT NULL,
  museum_id           INTEGER    NOT NULL,
  PRIMARY KEY (object_id),
  FOREIGN KEY (latest_move_id) REFERENCES MUSARK_STORAGE.EVENT (event_id),
  FOREIGN KEY (current_location_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE (storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_ACTOR (
  event_id NUMBER(20)     NOT NULL,
  role_id  INTEGER        NOT NULL,
  actor_uuid VARCHAR2(36) NOT NULL, -- reference by Id to the ActorService
  PRIMARY KEY (event_id, role_id, actor_uuid),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id),
  FOREIGN KEY (role_id)  REFERENCES MUSARK_STORAGE.ROLE (role_id)
);

CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_OBJECT (
  event_id      NUMBER(20) NOT NULL,
  role_id       INTEGER    NOT NULL,
  object_id     NUMBER(20) NOT NULL,
  event_type_id NUMBER(20) NOT NULL,
  PRIMARY KEY (event_id, role_id, object_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id),
  FOREIGN KEY (role_id) REFERENCES MUSARK_STORAGE.ROLE (role_id),
  FOREIGN KEY (object_id) REFERENCES MUSARK_STORAGE.LOCAL_OBJECT (object_id)
);

-- This is the generic event-to-place relation, the place of where an event
-- happened, the place of where something was moved to etc.
CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_PLACE (
  event_id      NUMBER(20) NOT NULL,
  role_id       INTEGER    NOT NULL,
  place_id      NUMBER(20) NOT NULL,
  event_type_id NUMBER(20) NOT NULL,
  PRIMARY KEY (event_id, role_id, place_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id),
  FOREIGN KEY (role_id) REFERENCES MUSARK_STORAGE.ROLE (role_id),
  FOREIGN KEY (place_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE (storage_node_id)
);

-- For some event types, the "objects" really are places. Read it as
-- Event_Role_PlaceAsObject. This situation is stored in this table. We could
-- have used the EVENT_ROLE_OBJECT, but then we would loose foreign keys and
-- would have needed to tag which is which.
CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_PLACE_AS_OBJECT (
  event_id      NUMBER(20) NOT NULL,
  role_id       INTEGER    NOT NULL,
  place_id      NUMBER(20) NOT NULL,
  event_type_id NUMBER(20) NOT NULL,
  PRIMARY KEY (event_id, role_id, place_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id),
  FOREIGN KEY (role_id) REFERENCES MUSARK_STORAGE.ROLE (role_id),
  FOREIGN KEY (place_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE (storage_node_id)
);

-- ===========================================================================
-- Tables for Events with additional attributes. These do not fit into the
-- three columns set aside for the custom attributes in the EVENT table.
-- ===========================================================================

-- Contains data for Events that have a FROM -> TO structure.
CREATE TABLE MUSARK_STORAGE.OBSERVATION_FROM_TO (
  event_id   NUMBER(20) NOT NULL,
  value_from NUMBER,
  value_to   NUMBER,
  PRIMARY KEY (event_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id)
);

-- Contains extra data for storing environment requirement.
CREATE TABLE MUSARK_STORAGE.E_ENVIRONMENT_REQUIREMENT (
  event_id          NUMBER(20) NOT NULL,
  temperature       NUMBER,
  temp_tolerance    INTEGER,
  rel_humidity      NUMBER,
  rel_hum_tolerance INTEGER,
  hypoxic_air       NUMBER,
  hyp_air_tolerance INTEGER,
  cleaning          VARCHAR2(250),
  light             VARCHAR2(250),
  PRIMARY KEY (event_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id)
);

-- Contains extra data for storing info about a pest lifecycle. It is used by
-- the ObservationPest event which can have many of these.
CREATE TABLE MUSARK_STORAGE.OBSERVATION_PEST_LIFECYCLE (
  event_id NUMBER(20) NOT NULL,
  stage    VARCHAR2(250),
  quantity INTEGER,
  FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT (event_id)
);

-- ===========================================================================
-- Pre-populating necessary data
-- ===========================================================================

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (1, 'MoveObject');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (2, 'MovePlace');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (3, 'EnvRequirement');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (4, 'Control');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (5, 'Observation');

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (6, 'ControlAlcohol');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (7, 'ControlCleaning');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (8, 'ControlGas');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (9, 'ControlHypoxicAir');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (10, 'ControlLightingCondition');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (11, 'ControlMold');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (12, 'ControlPest');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (13, 'ControlRelativeHumidity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (14, 'ControlTemperature');

INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (15, 'ObservationAlcohol');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (16, 'ObservationCleaning');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (17, 'ObservationFireProtection');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (18, 'ObservationGas');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (19, 'ObservationHypoxicAir');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (20, 'ObservationLightingCondition');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (21, 'ObservationMold');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (22, 'ObservationPerimeterSecurity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (23, 'ObservationRelativeHumidity');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (24, 'ObservationPest');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (25, 'ObservationTemperature');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (26, 'ObservationTheftProtection');
INSERT INTO MUSARK_STORAGE.EVENT_TYPE (event_type_id, name)
VALUES (27, 'ObservationWaterDamageAssessment');

INSERT INTO MUSARK_STORAGE.ROLE (role_id, name, type)
VALUES (1, 'DoneWith', 'object');

INSERT INTO MUSARK_STORAGE.ROLE (role_id, name, type)
VALUES (2, 'DoneBy', 'actor');

INSERT INTO MUSARK_STORAGE.ROLE (role_id, name, type)
VALUES (3, 'toPlace', 'storageNode');

INSERT INTO MUSARK_STORAGE.ROLE (role_id, name, type)
VALUES (4, 'fromPlace', 'storageNode')


# --- !Downs
