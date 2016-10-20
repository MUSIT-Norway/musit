# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSIT_MAPPING;

CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT (
  object_id NUMBER(20) NOT NULL,
  latest_move_id NUMBER(20),
  current_location_id INTEGER,
  museum_id INTEGER NOT NULL,
  PRIMARY KEY (object_id)
);

CREATE TABLE MUSARK_STORAGE.STORAGE_NODE (
  storage_node_id NUMBER(20) NOT NULL GENERATED ALWAYS AS IDENTITY,
  storage_node_name VARCHAR(512),
  area NUMBER,
  area_to NUMBER,
  is_storage_unit VARCHAR(1) DEFAULT '1',
  is_part_of NUMBER(20),
  height NUMBER,
  height_to NUMBER,
  node_path  VARCHAR(1000) not null,
  is_deleted INTEGER DEFAULT 0 NOT NULL,
  storage_type VARCHAR(100) DEFAULT 'StorageUnit',
  group_read VARCHAR(4000),
  group_write VARCHAR(4000),
  museum_id INTEGER NOT NULL,
  PRIMARY KEY (storage_node_id)
);

CREATE TABLE MUSIT_MAPPING.MUSITTHING (
  id NUMBER(20) NOT NULL GENERATED ALWAYS AS IDENTITY,
  museumNo VARCHAR NOT NULL,
  subNo VARCHAR,
  term VARCHAR NOT NULL,
  museumId INTEGER NOT NULL,
  museumNoAsNumber INTEGER,
  subNoAsNumber INTEGER
);

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID)
VALUES ('root-node', NULL, NULL, '1', NULL, NULL, NULL, false, 'Root', NULL, NULL, ',1,', 2);

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID)
VALUES ('Utviklingsmuseet', NULL, NULL, '1', 1, NULL, NULL, false, 'Organisation', NULL, NULL, ',1,2,', 2);

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID)
VALUES ('Forskningens hus', NULL, NULL, '1', 2, NULL, NULL, false, 'Building', NULL, NULL, ',1,2,3,', 2);

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID)
VALUES ('Kulturværelset', NULL, NULL, '1', 3, NULL, NULL, false, 'Room', NULL, NULL, ',1,2,3,4,', 2);

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID)
VALUES ('Naturværelset', NULL, NULL, '1', 3, NULL, NULL, false, 'Room', NULL, NULL, ',1,2,3,5,', 2);

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_NAME, AREA, AREA_TO, IS_STORAGE_UNIT, IS_PART_OF, HEIGHT, HEIGHT_TO, IS_DELETED, STORAGE_TYPE, GROUP_READ, GROUP_WRITE, NODE_PATH, MUSEUM_ID)
VALUES ('Forskningsværelset', NULL, NULL, '1', 3, NULL, NULL, false, 'Room', NULL, NULL, ',1,2,3,6,', 2);


INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES ('C666', '34', 'Øks', 2, 666);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (1, 23, 3, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES ('C666', '31', 'Sverd', 2, 666);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (2, 23, 3, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES ('C666', '38', 'Sommerfugl', 2, 666);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (3, 23, 3, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '1a', 'Øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (4, 23, 4, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '2a', 'Skummel øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (5, 23, 4, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '3', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (6, 23, 4, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '4', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (7, 23, 4, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '5', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (8, 23, 4, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '6', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (9, 23, 4, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '7', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (10, 23, 4, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '8', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (11, 23, 4, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '9', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (12, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '10a', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (13, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '11', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (14, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '12', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (15, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '13', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (16, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '14', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (17, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '15', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (18, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '16', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (19, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '17', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (20, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '18', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (21, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '19', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (22, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '20b', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (23, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '22', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (24, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '21', 'Fin øks', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (25, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C2', '', 'Sverd', 2, 1);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (26, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C777', '35', 'Øks', 2, 777);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (27, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C.777', '34B', 'Øks', 2, 777);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (28, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C.777', '34', 'Øks', 2, 777);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (29, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C.777', '34A', 'Øks', 2, 777);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (30, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555', '34B', 'Øks', 2, 555);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (31, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555', '34A', 'Øks', 2, 555);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (32, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555', '34C', 'Øks', 2, 555);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (33, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555A', 'B', 'Øks', 2, 555);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (34, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555B', 'A', 'Øks', 2, 555);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (35, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555C', 'C', 'Øks', 2, 555);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (36, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C888_B', 'B', 'Øks', 2, 888);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (37, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C888_A', 'A', 'Øks', 2, 888);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (38, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C888xC', 'C', 'Øks', 2, 888);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (39, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81%A', 'A', 'Bøtte', 2, 81);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (40, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81%XA', 'B', 'Bøtte', 2, 81);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (41, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81-A', 'A', 'Bøtte', 2, 81);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (42, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81-XA', 'B', 'Bøtte', 2, 81);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (43, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81¤A', 'A', 'Bøtte', 2, 81);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (44, 23, 5, 2);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81¤XA', 'B', 'Bøtte', 2, 81);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (45, 23, 5, 2);
