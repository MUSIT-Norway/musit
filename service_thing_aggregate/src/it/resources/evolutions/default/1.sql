# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSIT_MAPPING;

CREATE TABLE MUSIT_MAPPING.VIEW_MUSITTHING (
  id NUMBER(20) NOT NULL GENERATED ALWAYS AS IDENTITY,
  displayId VARCHAR NOT NULL,
  displayName VARCHAR,
  PRIMARY KEY (id)
);

CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT (
  object_id NUMBER(20) NOT NULL,
  latest_move_id NUMBER(20),
  current_location_id INTEGER,
  museum_id INTEGER NOT NULL,
  FOREIGN KEY(object_id) REFERENCES MUSIT_MAPPING.VIEW_MUSITTHING(id)
);

CREATE TABLE MUSARK_STORAGE.STORAGE_NODE (
  storage_node_id NUMBER(20) NOT NULL GENERATED ALWAYS AS IDENTITY,
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

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (storage_node_id, museum_id)
VALUES (3, 2);

INSERT INTO MUSIT_MAPPING.VIEW_MUSITTHING (id, displayId, displayName)
VALUES (1, 'C666/34', 'Øks');
INSERT INTO MUSIT_MAPPING.VIEW_MUSITTHING (id, displayId, displayName)
VALUES (2, 'C666/31', 'Sverd');
INSERT INTO MUSIT_MAPPING.VIEW_MUSITTHING (id, displayId, displayName)
VALUES (3, 'C666/38', 'Sommerfugl');

INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (1, 23, 3, 2);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (2, 23, 3, 2);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id)
VALUES (3, 23, 3, 2);


INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES ('C666', '34', 'Øks', 1, 666);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '1a', 'Øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '2a', 'Skummel øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '3', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '4', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '5', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '6', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '7', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '8', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '9', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '10a', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '11', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '12', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '13', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '14', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '15', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '16', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '17', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '18', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '19', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '20b', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '22', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C1', '21', 'Fin øks', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C2', '', 'Sverd', 1, 1);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C777', '35', 'Øks', 1, 777);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C.777', '34B', 'Øks', 1, 777);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C.777', '34', 'Øks', 1, 777);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C.777', '34A', 'Øks', 1, 777);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555', '34B', 'Øks', 1, 555);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555', '34A', 'Øks', 1, 555);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555', '34C', 'Øks', 1, 555);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555A', 'B', 'Øks', 1, 555);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555B', 'A', 'Øks', 1, 555);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C555C', 'C', 'Øks', 1, 555);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C888_B', 'B', 'Øks', 1, 888);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C888_A', 'A', 'Øks', 1, 888);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C888xC', 'C', 'Øks', 1, 888);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81%A', 'A', 'Bøtte', 1, 81);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81%XA', 'B', 'Bøtte', 1, 81);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81-A', 'A', 'Bøtte', 1, 81);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81-XA', 'B', 'Bøtte', 1, 81);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81¤A', 'A', 'Bøtte', 1, 81);

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber)
VALUES('C81¤XA', 'B', 'Bøtte', 1, 81);