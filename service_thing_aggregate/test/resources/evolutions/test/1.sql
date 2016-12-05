# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSIT_MAPPING;

CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT (
  object_id           NUMBER(20) NOT NULL,
  latest_move_id      NUMBER(20),
  current_location_id INTEGER,
  museum_id           INTEGER    NOT NULL,
  PRIMARY KEY (object_id)
);

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
  old_barcode       INTEGER,
  museum_id         INTEGER           NOT NULL,
  updated_by        VARCHAR2(36)      NOT NULL,
  updated_date      TIMESTAMP         NOT NULL, -- When the change was received by the system
  PRIMARY KEY (storage_node_id)
);

CREATE TABLE MUSIT_MAPPING.MUSITTHING (
  object_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  museumNo         VARCHAR(100) NOT NULL,
  subNo            VARCHAR(100),
  term             VARCHAR(256) NOT NULL,
  museumId         INTEGER      NOT NULL,
  museumNoAsNumber INTEGER,
  subNoAsNumber    INTEGER,
  mainobject_id    NUMBER(20),
  old_schema_name  VARCHAR(500),
  PRIMARY KEY (object_id)
);

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE)
VALUES ('root-node', NULL, NULL, '1', NULL, NULL, NULL, FALSE, 'Root', NULL, NULL, ',1,', 99, '896125d3-0563-46b6-a7c5-51f3f899ff0a', '2016-01-01 00:00:00');

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE)
VALUES ('Utviklingsmuseet', NULL, NULL, '1', 1, NULL, NULL, FALSE, 'Organisation', NULL, NULL, ',1,2,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE)
VALUES ('Forskningens hus', NULL, NULL, '1', 2, NULL, NULL, FALSE, 'Building', NULL, NULL, ',1,2,3,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE)
VALUES ('Kulturværelset', NULL, NULL, '1', 3, NULL, NULL, FALSE, 'Room', NULL, NULL, ',1,2,3,4,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE)
VALUES ('Naturværelset', NULL, NULL, '1', 3, NULL, NULL, FALSE, 'Room', NULL, NULL, ',1,2,3,5,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE)
VALUES ('Forskningsværelset', NULL, NULL, '1', 3, NULL, NULL, FALSE, 'Room', NULL, NULL, ',1,2,3,6,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');


INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES ('C666', '34', 'Øks', 99, 666, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (1, 23, 3, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES ('C666', '31', 'Sverd', 99, 666, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (2, 23, 3, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES ('C666', '38', 'Sommerfugl', 99, 666, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (3, 23, 3, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '1a', 'Øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (4, 23, 4, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '2a', 'Skummel øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (5, 23, 4, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '3', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (6, 23, 4, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '4', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (7, 23, 4, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '5', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (8, 23, 4, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '6', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (9, 23, 4, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '7', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (10, 23, 4, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '8', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (11, 23, 4, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '9', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (12, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '10a', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (13, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '11', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (14, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '12', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (15, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '13', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (16, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '14', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (17, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '15', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (18, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '16', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (19, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '17', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (20, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '18', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (21, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '19', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (22, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '20b', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (23, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '22', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (24, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C1', '21', 'Fin øks', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (25, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C2', '', 'Sverd', 99, 1, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (26, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C777', '35', 'Øks', 99, 777, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (27, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C.777', '34B', 'Øks', 99, 777, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (28, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C.777', '34', 'Øks', 99, 777, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (29, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C.777', '34A', 'Øks', 99, 777, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (30, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C555', '34B', 'Øks', 99, 555, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (31, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C555', '34A', 'Øks', 99, 555, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (32, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C555', '34C', 'Øks', 99, 555, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (33, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C555A', 'B', 'Øks', 99, 555, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (34, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C555B', 'A', 'Øks', 99, 555, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (35, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C555C', 'C', 'Øks', 99, 555, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (36, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C888_B', 'B', 'Øks', 99, 888, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (37, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C888_A', 'A', 'Øks', 99, 888, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (38, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C888xC', 'C', 'Øks', 99, 888, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (39, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C81%A', 'A', 'Bøtte', 99, 81, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (40, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C81%XA', 'B', 'Bøtte', 99, 81, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (41, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C81-A', 'A', 'Bøtte', 99, 81, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (42, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C81-XA', 'B', 'Bøtte', 99, 81, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (43, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C81¤A', 'A', 'Bøtte', 99, 81, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (44, 23, 5, 99);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('C81¤XA', 'B', 'Bøtte', 99, 81, 'USD_ARK_GJENSTAND_O');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (45, 23, 5, 99);

-- Numismatikk
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('F555', '55A', 'Gammel mynt', 99, 555, 'USD_NUMISMATIKK');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (46, 23, 5, 99);

-- Lav
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schema_name)
VALUES('L234', '', 'Reinlav', 99, 234, 'MUSIT_BOTANIKK_LAV');
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (47, 23, 5, 99);