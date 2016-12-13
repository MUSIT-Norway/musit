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
  storage_node_id   NUMBER(20)        GENERATED BY DEFAULT AS IDENTITY,
  storage_node_uuid VARCHAR2(36)      NOT NULL,
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
  object_id         NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  museumNo          VARCHAR(100) NOT NULL,
  subNo             VARCHAR(100),
  term              VARCHAR(256) NOT NULL,
  museumId          INTEGER      NOT NULL,
  museumNoAsNumber  INTEGER,
  subNoAsNumber     INTEGER,
  mainobject_id     NUMBER(20),
  old_schemaname    VARCHAR(500),
  lokal_pk          NUMBER(20),
  new_collection_id INTEGER,
  PRIMARY KEY (object_id)
);

INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('dca44956-40d0-48dc-bd0d-921b825ad019', 'Utviklingsmuseet'    , NULL, 'Root'        , ',1,'      , 99, '896125d3-0563-46b6-a7c5-51f3f899ff0a', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('b56b654a-6de3-442f-97af-ca6d806cc5a6', 'Utenfor museet'      , NULL, 'RootLoan'    , ',2,'      , 99, '896125d3-0563-46b6-a7c5-51f3f899ff0a', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('d3982b48-56c7-4d27-bc81-6e38b59d57ed', 'Utviklingsmuseet Org', 1   , 'Organisation', ',1,3,'    , 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('3562e09e-6cf4-4b27-acad-e655e771c016', 'Forskningens hus'    , 3   , 'Building'    , ',1,3,4,'  , 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('01134afe-b262-434b-a71f-8f697bc75e56', 'Kulturværelset'      , 4   , 'Room'        , ',1,3,4,5,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('244f09a3-eb1a-49e7-80ee-7a07baa016dd', 'Naturværelset'       , 4   , 'Room'        , ',1,3,4,6,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('6e5b9810-9bbf-464a-a0b9-c27f6095ba0c', 'Forskningsværelset'  , 4   , 'Room'        , ',1,3,4,7,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('45b37f3f-a007-4372-97c2-2e58b237fb8e', 'British museum'      , 2   , 'Organisation', ',2,8'     , 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('82a48fa0-83b0-463e-960b-af668d64def2', 'The Louvre'          , 2   , 'Organisation', ',2,9'     , 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('02accb0d-71f6-498e-9207-c3309f39ee8d', 'Death Star gallery'  , 2   , 'Organisation', ',2,10'    , 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('4e93e2c8-0cbd-49f2-ab1f-427d5ccddec2', 'Utenfor 2'           , NULL, 'RootLoan'    , ',11,'     , 99, '896125d3-0563-46b6-a7c5-51f3f899ff0a', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO MUSARK_STORAGE.STORAGE_NODE (STORAGE_NODE_UUID, STORAGE_NODE_NAME, IS_PART_OF, STORAGE_TYPE, NODE_PATH, MUSEUM_ID, UPDATED_BY, UPDATED_DATE) VALUES ('5fef94bc-aa79-4151-b4a2-658e89a949a4', 'FooBar of History'   , 11  , 'Organisation', ',11,12,'  , 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', TO_DATE('2016-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));

INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C666'  , '34' , 'Øks'        , 99, 666, 'USD_ARK_GJENSTAND_O', 100, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C666'  , '31' , 'Sverd'      , 99, 666, 'USD_ARK_GJENSTAND_O', 101, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C666'  , '38' , 'Sommerfugl' , 99, 666, 'USD_ARK_GJENSTAND_O', 102, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '1a' , 'Øks'        , 99, 1  , 'USD_ARK_GJENSTAND_O', 103, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '2a' , 'Skummel øks', 99, 1  , 'USD_ARK_GJENSTAND_O', 104, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '3'  , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 105, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '4'  , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 106, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '5'  , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 107, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '6'  , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 108, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '7'  , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 109, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '8'  , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 110, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '9'  , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 111, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '10a', 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 112, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '11' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 113, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '12' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 114, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '13' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 115, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '14' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 116, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '15' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 117, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '16' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 118, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '17' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 119, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '18' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 120, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '19' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 121, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '20b', 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 122, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '22' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 123, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C1'    , '21' , 'Fin øks'    , 99, 1  , 'USD_ARK_GJENSTAND_O', 124, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C2'    , ''   , 'Sverd'      , 99, 1  , 'USD_ARK_GJENSTAND_O', 125, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C777'  , '35' , 'Øks'        , 99, 777, 'USD_ARK_GJENSTAND_O', 126, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C.777' , '34B', 'Øks'        , 99, 777, 'USD_ARK_GJENSTAND_O', 127, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C.777' , '34' , 'Øks'        , 99, 777, 'USD_ARK_GJENSTAND_O', 128, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C.777' , '34A', 'Øks'        , 99, 777, 'USD_ARK_GJENSTAND_O', 129, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C555'  , '34B', 'Øks'        , 99, 555, 'USD_ARK_GJENSTAND_O', 130, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C555'  , '34A', 'Øks'        , 99, 555, 'USD_ARK_GJENSTAND_O', 131, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C555'  , '34C', 'Øks'        , 99, 555, 'USD_ARK_GJENSTAND_O', 132, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C555A' , 'B'  , 'Øks'        , 99, 555, 'USD_ARK_GJENSTAND_O', 133, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C555B' , 'A'  , 'Øks'        , 99, 555, 'USD_ARK_GJENSTAND_O', 134, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C555C' , 'C'  , 'Øks'        , 99, 555, 'USD_ARK_GJENSTAND_O', 135, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C888_B', 'B'  , 'Øks'        , 99, 888, 'USD_ARK_GJENSTAND_O', 136, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C888_A', 'A'  , 'Øks'        , 99, 888, 'USD_ARK_GJENSTAND_O', 137, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C888xC', 'C'  , 'Øks'        , 99, 888, 'USD_ARK_GJENSTAND_O', 138, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C81%A' , 'A'  , 'Bøtte'      , 99, 81 , 'USD_ARK_GJENSTAND_O', 139, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C81%XA', 'B'  , 'Bøtte'      , 99, 81 , 'USD_ARK_GJENSTAND_O', 140, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C81-A' , 'A'  , 'Bøtte'      , 99, 81 , 'USD_ARK_GJENSTAND_O', 141, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C81-XA', 'B'  , 'Bøtte'      , 99, 81 , 'USD_ARK_GJENSTAND_O', 142, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C81¤A' , 'A'  , 'Bøtte'      , 99, 81 , 'USD_ARK_GJENSTAND_O', 143, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('C81¤XA', 'B'  , 'Bøtte'      , 99, 81 , 'USD_ARK_GJENSTAND_O', 144, 1);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('F555'  , '55A', 'Gammel mynt', 99, 555, 'USD_NUMISMATIKK'    , 145, 3);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('L234'  , ''   , 'Reinlav'    , 99, 234, 'MUSIT_BOTANIKK_LAV' , 146, 4);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id, mainobject_id) VALUES ('K123'  , ''   , 'Drakt'      , 99, 123, 'USD_ARK_GJENSTAND_O', 147, 1, 12);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id, mainobject_id) VALUES ('K123'  , ''   , 'Skjorte'    , 99, 123, 'USD_ARK_GJENSTAND_O', 148, 1, 12);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id, mainobject_id) VALUES ('K123'  , ''   , 'Kjole'      , 99, 123, 'USD_ARK_GJENSTAND_O', 149, 1, 12);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('L234'  , ''   , 'Kartlav'    , 99, 234, 'MUSIT_BOTANIKK_LAV'  , 150, 4);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('L234'  , ''   , 'Fokklav'    , 99, 234, 'MUSIT_BOTANIKK_LAV'  , 151, 4);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('L234'  , ''   , 'Grønnever'  , 99, 234, 'MUSIT_BOTANIKK_LAV'  , 152, 4);
INSERT INTO MUSIT_MAPPING.MUSITTHING (museumNo, subNo, term, museumId, museumNoAsNumber, old_schemaname, lokal_pk, new_collection_id) VALUES ('L234'  , ''   , 'Islandslav' , 99, 234, 'MUSIT_BOTANIKK_LAV'  , 153, 4);



INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (1 , 23, 4, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (2 , 23, 4, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (3 , 23, 4, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (4 , 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (5 , 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (6 , 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (7 , 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (8 , 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (9 , 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (10, 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (11, 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (12, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (13, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (14, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (15, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (16, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (17, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (18, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (19, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (20, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (21, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (22, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (23, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (24, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (25, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (26, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (27, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (28, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (29, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (30, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (31, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (32, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (33, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (34, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (35, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (36, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (37, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (38, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (39, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (40, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (41, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (42, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (43, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (44, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (45, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (46, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (47, 23, 6, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (48, 23, 7, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (49, 23, 7, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (50, 23, 7, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (51, 23, 4, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (52, 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (53, 23, 5, 99);
INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT (object_id, latest_move_id, current_location_id, museum_id) VALUES (54, 23, 5, 99);
