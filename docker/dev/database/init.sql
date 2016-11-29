-- ===========================================================================
-- Tables for auth stuff...
-- ===========================================================================
CREATE SCHEMA IF NOT EXISTS "MUSARK_AUTH";


CREATE TABLE "MUSARK_AUTH"."AUTH_GROUP" (
  "GROUP_UUID" VARCHAR(36) NOT NULL,
  "GROUP_NAME" VARCHAR(100) NOT NULL,
  "GROUP_PERMISSION" INTEGER NOT NULL,
  "GROUP_MUSEUMID" INTEGER NOT NULL,
  "GROUP_DESCRIPTION" VARCHAR(512),
  PRIMARY KEY ("GROUP_UUID"),
  CONSTRAINT UNIQUE_GROUP_NAME UNIQUE ("GROUP_NAME")
);

CREATE TABLE "MUSARK_AUTH"."USER_AUTH_GROUP" (
  "USER_FEIDE_EMAIL" VARCHAR(254) NOT NULL,
  "GROUP_UUID" VARCHAR(36) NOT NULL,
  PRIMARY KEY ("USER_FEIDE_EMAIL", "GROUP_UUID"),
  FOREIGN KEY ("GROUP_UUID") REFERENCES "MUSARK_AUTH"."AUTH_GROUP" ("GROUP_UUID")
);

CREATE TABLE "MUSARK_AUTH"."USER_INFO" (
  "USER_UUID" VARCHAR(36) NOT NULL,
  "SECONDARY_ID" VARCHAR(512),
  "NAME" VARCHAR(512),
  "EMAIL" VARCHAR(254),
  "PICTURE" VARCHAR(100),
  PRIMARY KEY ("USER_UUID")
);



-- ===========================================================================
-- Tables for storage nodes
-- ===========================================================================
CREATE SCHEMA IF NOT EXISTS "MUSARK_STORAGE";


CREATE TABLE "MUSARK_STORAGE"."STORAGE_NODE" (
  "STORAGE_NODE_ID" BIGSERIAL,
  "STORAGE_NODE_NAME" VARCHAR(512),
  "AREA" DOUBLE PRECISION,
  "AREA_TO" DOUBLE PRECISION,
  "IS_STORAGE_UNIT" VARCHAR(1) DEFAULT '1',
  "IS_PART_OF" BIGINT,
  "HEIGHT" DOUBLE PRECISION,
  "HEIGHT_TO" DOUBLE PRECISION,
  "IS_DELETED" BOOLEAN NOT NULL,
  "STORAGE_TYPE" VARCHAR(100) DEFAULT 'StorageUnit',
  "GROUP_READ" VARCHAR(4000),
  "GROUP_WRITE" VARCHAR(4000),
  "NODE_PATH" VARCHAR(4000),
  "OLD_BARCODE" INTEGER,
  "MUSEUM_ID" INTEGER NOT NULL,
  "UPDATED_BY" VARCHAR(36) NOT NULL,
  "UPDATED_DATE" TIMESTAMP NOT NULL,
  PRIMARY KEY ("STORAGE_NODE_ID")
);

CREATE TABLE "MUSARK_STORAGE"."ROOM" (
  "STORAGE_NODE_ID" BIGINT NOT NULL,
  "PERIMETER_SECURITY" BOOLEAN,
  "THEFT_PROTECTION" BOOLEAN,
  "FIRE_PROTECTION" BOOLEAN,
  "WATER_DAMAGE_ASSESSMENT" BOOLEAN,
  "ROUTINES_AND_CONTINGENCY_PLAN" BOOLEAN,
  "RELATIVE_HUMIDITY" BOOLEAN,
  "TEMPERATURE_ASSESSMENT" BOOLEAN,
  "LIGHTING_CONDITION" BOOLEAN,
  "PREVENTIVE_CONSERVATION" BOOLEAN,
  PRIMARY KEY ("STORAGE_NODE_ID"),
  FOREIGN KEY ("STORAGE_NODE_ID") REFERENCES "MUSARK_STORAGE"."STORAGE_NODE"("STORAGE_NODE_ID")
);

CREATE TABLE "MUSARK_STORAGE"."BUILDING" (
  "STORAGE_NODE_ID" BIGINT NOT NULL,
  "POSTAL_ADDRESS" VARCHAR(512),
  PRIMARY KEY ("STORAGE_NODE_ID"),
  FOREIGN KEY ("STORAGE_NODE_ID") REFERENCES "MUSARK_STORAGE"."STORAGE_NODE"("STORAGE_NODE_ID")
);

CREATE TABLE "MUSARK_STORAGE"."ORGANISATION"(
  "STORAGE_NODE_ID" BIGINT NOT NULL,
  "POSTAL_ADDRESS"  VARCHAR(512),
  PRIMARY KEY ("STORAGE_NODE_ID"),
  FOREIGN KEY ("STORAGE_NODE_ID") REFERENCES "MUSARK_STORAGE"."STORAGE_NODE"("STORAGE_NODE_ID")
);

-- ===========================================================================
-- Event specific tables.
-- ===========================================================================

CREATE TABLE "MUSARK_STORAGE"."ROLE" (
  "ROLE_ID" SERIAL,
  "NAME" VARCHAR(100) NOT NULL,
  "DATA_TYPE" VARCHAR(100),
  PRIMARY KEY ("ROLE_ID")
);

CREATE TABLE "MUSARK_STORAGE"."EVENT_TYPE" (
  "EVENT_TYPE_ID" SERIAL,
  "NAME" VARCHAR(100) NOT NULL,
  PRIMARY KEY ("EVENT_TYPE_ID")
);

CREATE TABLE "MUSARK_STORAGE"."EVENT" (
  "EVENT_ID" BIGSERIAL,
  "EVENT_TYPE_ID" INTEGER NOT NULL,
  "NOTE" VARCHAR(500),
  "EVENT_DATE" TIMESTAMP(0) NOT NULL,
  "REGISTERED_BY" VARCHAR(36) NOT NULL,
  "REGISTERED_DATE" TIMESTAMP NOT NULL,
  "VALUE_LONG" BIGINT,
  "VALUE_STRING" VARCHAR(250),
  "VALUE_FLOAT" DOUBLE PRECISION,
  "PART_OF" BIGINT,
  PRIMARY KEY ("EVENT_ID"),
  FOREIGN KEY ("EVENT_TYPE_ID") REFERENCES "MUSARK_STORAGE"."EVENT_TYPE"("EVENT_TYPE_ID"),
  FOREIGN KEY ("PART_OF") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID")
);

-- ???
CREATE TABLE "MUSARK_STORAGE"."EVENT_RELATION_EVENT" (
  "FROM_EVENT_ID" BIGINT NOT NULL,
  "RELATION_ID" INTEGER NOT NULL,
  "TO_EVENT_ID" BIGINT NOT NULL,
  FOREIGN KEY ("FROM_EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID"),
  FOREIGN KEY ("TO_EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID")
);

CREATE TABLE "MUSARK_STORAGE"."LOCAL_OBJECT" (
  "OBJECT_ID" BIGSERIAL,
  "LATEST_MOVE_ID" BIGINT NOT NULL,
  "CURRENT_LOCATION_ID" BIGINT NOT NULL,
  "MUSEUM_ID" INTEGER NOT NULL,
  PRIMARY KEY ("OBJECT_ID")
);

CREATE TABLE "MUSARK_STORAGE"."EVENT_ROLE_ACTOR" (
  "EVENT_ID" BIGINT NOT NULL,
  "ROLE_ID" INTEGER NOT NULL,
  "ACTOR_UUID" VARCHAR(36) NOT NULL,
  PRIMARY KEY ("EVENT_ID", "ROLE_ID", "ACTOR_UUID"),
  FOREIGN KEY ("EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID"),
  FOREIGN KEY ("ROLE_ID") REFERENCES "MUSARK_STORAGE"."ROLE"("ROLE_ID")
);

CREATE TABLE "MUSARK_STORAGE"."EVENT_ROLE_OBJECT" (
  "EVENT_ID" BIGINT NOT NULL,
  "ROLE_ID" INTEGER NOT NULL,
  "OBJECT_ID" BIGINT NOT NULL,
  "EVENT_TYPE_ID" BIGINT NOT NULL,
  PRIMARY KEY ("EVENT_ID", "ROLE_ID", "OBJECT_ID"),
  FOREIGN KEY ("EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID"),
  FOREIGN KEY ("ROLE_ID") REFERENCES "MUSARK_STORAGE"."ROLE"("ROLE_ID"),
  FOREIGN KEY ("OBJECT_ID") REFERENCES "MUSARK_STORAGE"."LOCAL_OBJECT"("OBJECT_ID")
);

CREATE TABLE "MUSARK_STORAGE"."EVENT_ROLE_PLACE" (
  "EVENT_ID" BIGINT NOT NULL,
  "ROLE_ID" INTEGER NOT NULL,
  "PLACE_ID"  BIGINT NOT NULL,
  "EVENT_TYPE_ID" BIGINT NOT NULL,
  PRIMARY KEY ("EVENT_ID", "ROLE_ID", "PLACE_ID"),
  FOREIGN KEY ("EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID"),
  FOREIGN KEY ("ROLE_ID") REFERENCES "MUSARK_STORAGE"."ROLE"("ROLE_ID"),
  FOREIGN KEY ("PLACE_ID") REFERENCES "MUSARK_STORAGE"."STORAGE_NODE"("STORAGE_NODE_ID")
);

CREATE TABLE "MUSARK_STORAGE"."EVENT_ROLE_PLACE_AS_OBJECT" (
  "EVENT_ID" BIGINT NOT NULL,
  "ROLE_ID" INTEGER NOT NULL,
  "PLACE_ID"  BIGINT NOT NULL,
  "EVENT_TYPE_ID" BIGINT NOT NULL,
  PRIMARY KEY ("EVENT_ID", "ROLE_ID", "PLACE_ID"),
  FOREIGN KEY ("EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID"),
  FOREIGN KEY ("ROLE_ID") REFERENCES "MUSARK_STORAGE"."ROLE"("ROLE_ID"),
  FOREIGN KEY ("PLACE_ID") REFERENCES "MUSARK_STORAGE"."STORAGE_NODE"("STORAGE_NODE_ID")
);

CREATE TABLE "MUSARK_STORAGE"."OBSERVATION_FROM_TO" (
  "EVENT_ID" BIGINT NOT NULL,
  "VALUE_FROM" DOUBLE PRECISION,
  "VALUE_TO" DOUBLE PRECISION,
  PRIMARY KEY ("EVENT_ID"),
  FOREIGN KEY ("EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID")
);

CREATE TABLE "MUSARK_STORAGE"."E_ENVIRONMENT_REQUIREMENT" (
  "EVENT_ID" BIGINT NOT NULL,
  "TEMPERATURE" DOUBLE PRECISION,
  "TEMP_TOLERANCE" INTEGER,
  "REL_HUMIDITY" DOUBLE PRECISION,
  "REL_HUM_TOLERANCE" INTEGER,
  "HYPOXIC_AIR" DOUBLE PRECISION,
  "HYP_AIR_TOLERANCE" INTEGER,
  "CLEANING" VARCHAR(250),
  "LIGHT" VARCHAR(250),
  PRIMARY KEY ("EVENT_ID"),
  FOREIGN KEY ("EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID")
);

CREATE TABLE "MUSARK_STORAGE"."OBSERVATION_PEST_LIFECYCLE" (
  "EVENT_ID" BIGINT NOT NULL,
  "STAGE" VARCHAR(250),
  "QUANTITY" INTEGER,
  FOREIGN KEY ("EVENT_ID") REFERENCES "MUSARK_STORAGE"."EVENT"("EVENT_ID")
);

-- ===========================================================================
-- Actor specific tables.
-- ===========================================================================

CREATE SCHEMA IF NOT EXISTS "MUSIT_MAPPING";

CREATE TABLE "MUSIT_MAPPING"."ACTOR" (
  "ACTORID" BIGSERIAL,
  "ACTORNAME" VARCHAR(512),
  "DATAPORTEN_UUID" VARCHAR(36),
  "DATAPORTEN_USERNAME" VARCHAR(100),
  "OLD_USERNAME" VARCHAR(50),
  "LOKAL_PK" INTEGER,
  "TABELLID" INTEGER,
  "OLD_SCHEMANAME" VARCHAR(50),
  "MUSEUM_ID" INTEGER,
  "APPLICATION_UUID" VARCHAR(36),
  PRIMARY KEY ("ACTORID")
);

CREATE SCHEMA IF NOT EXISTS "MUSARK_ACTOR";

CREATE TABLE "MUSARK_ACTOR"."ORGANIZATION" (
  "ORG_ID" BIGSERIAL,
  "FULL_NAME" VARCHAR(255) NOT NULL,
  "NICKNAME" VARCHAR(255),
  "TEL" VARCHAR(20),
  "WEB" VARCHAR(255),
  PRIMARY KEY ("ORG_ID")
);

CREATE TABLE "MUSARK_ACTOR"."ORGANIZATION_ADDRESS" (
  "ORGADDRESSID" BIGSERIAL,
  "ORG_ID" BIGINT NOT NULL,
  "ADDRESS_TYPE" VARCHAR(20),
  "STREET_ADDRESS" VARCHAR(20),
  "LOCALITY" VARCHAR(255),
  "POSTAL_CODE" VARCHAR(12),
  "COUNTRY_NAME" VARCHAR(255),
  "LATITUDE" DOUBLE PRECISION,
  "LONGITUDE" DOUBLE PRECISION,
  PRIMARY KEY ("ORGADDRESSID"),
  FOREIGN KEY ("ORG_ID") REFERENCES "MUSARK_ACTOR"."ORGANIZATION"("ORG_ID")
);

-- ===========================================================================
-- Object specific tables.
-- ===========================================================================

CREATE SCHEMA IF NOT EXISTS MUSIT_MAPPING;

CREATE TABLE "MUSIT_MAPPING"."MUSITTHING" (
  "OBJECT_ID" BIGSERIAL,
  "MUSEUMNO" VARCHAR NOT NULL,
  "SUBNO" VARCHAR,
  "TERM" VARCHAR NOT NULL,
  "MUSEUMID" INTEGER NOT NULL,
  "MUSEUMNOASNUMBER" BIGINT,
  "SUBNOASNUMBER" BIGINT,
  "MAINOBJECT_ID" BIGINT,
  PRIMARY KEY ("OBJECT_ID")
 );


-- ===========================================================================
-- Pre-populating necessary data
-- ===========================================================================
SET search_path = "MUSARK_AUTH", pg_catalog;

INSERT INTO "AUTH_GROUP" VALUES ('c0f20097-e803-4d1f-9d86-7b36bcfaec19', 'GodAccess', 10000, 10000, 'Full blown administrator with access to all functionality');
INSERT INTO "AUTH_GROUP" VALUES ('869cc344-5515-49c0-a7a5-79e371f4e64b', 'TestAppAdmin', 40, 10000, 'Application administrator with access to all functionality');
INSERT INTO "AUTH_GROUP" VALUES ('2d503a2e-2211-45dd-a99f-fe1a38b5f2a2', 'TestSfRead',   10,    99, 'Read access to storage facility for TEST');
INSERT INTO "AUTH_GROUP" VALUES ('c81c314c-0675-4cd1-8956-c96a7163825b', 'TestSfWrite',  20,    99, 'Write access to storage facility for TEST');
INSERT INTO "AUTH_GROUP" VALUES ('bc4b4d44-9470-4622-8e29-03f0bfaf5149', 'TestSfAdmin',  30,    99, 'Admin access to storage facility for TEST');
INSERT INTO "AUTH_GROUP" VALUES ('3ce7692d-2101-45a4-955b-0ca861540cd9', 'KhmSfRead',    10,     3, 'Read access to storage facility for KHM');
INSERT INTO "AUTH_GROUP" VALUES ('fd34b019-81e1-47a2-987b-64389d6fce04', 'KhmSfWrite',   20,     3, 'Write access to storage facility for KHM');
INSERT INTO "AUTH_GROUP" VALUES ('de48b2dd-f25c-4b06-a5d4-7de45780ef2e', 'KhmSfAdmin',   30,     3, 'Admin access to storage facility for KHM');
INSERT INTO "AUTH_GROUP" VALUES ('92e3b487-c962-43d9-a2ea-b8a7bed1b67a', 'NhmSfRead',    10,     4, 'Read access to storage facility for NHM');
INSERT INTO "AUTH_GROUP" VALUES ('f311dd04-89b8-48a4-b4cd-68092f76ebab', 'NhmSfWrite',   20,     4, 'Write access to storage facility for NHM');
INSERT INTO "AUTH_GROUP" VALUES ('5aa16994-92b3-45df-adfa-7f9fdf54ce34', 'NhmSfAdmin',   30,     4, 'Admin access to storage facility for NHM');
INSERT INTO "AUTH_GROUP" VALUES ('b923b5df-54d3-4724-9386-c1273561f1a1', 'UmSfRead',     10,     2, 'Read access to storage facility for UM');
INSERT INTO "AUTH_GROUP" VALUES ('6ae158a1-398c-4f05-9992-1e19759e3221', 'UmSfWrite',    20,     2, 'Write access to storage facility for UM');
INSERT INTO "AUTH_GROUP" VALUES ('0ccefaff-aee3-4447-98e5-ec29bb9b80b7', 'UmSfAdmin',    30,     2, 'Admin access to storage facility for UM');
INSERT INTO "AUTH_GROUP" VALUES ('428f6108-f376-47e5-80c2-0e166e75ae42', 'AmSfRead',     10,     1, 'Read access to storage facility for AM');
INSERT INTO "AUTH_GROUP" VALUES ('f6a43a29-c0ee-419f-b315-d343629fb9b8', 'AmSfWrite',    20,     1, 'Write access to storage facility for AM');
INSERT INTO "AUTH_GROUP" VALUES ('3a3a173e-cf99-4301-9988-78fcd5d3d153', 'AmSfAdmin',    30,     1, 'Admin access to storage facility for AM');
INSERT INTO "AUTH_GROUP" VALUES ('3eb0b341-6c16-46f7-bd75-b0e12ba6cb8b', 'VmSfRead',     10,     5, 'Read access to storage facility for VM');
INSERT INTO "AUTH_GROUP" VALUES ('b4beaefb-d124-4f10-874d-ef59ffa1bb3b', 'VmSfWrite',    20,     5, 'Write access to storage facility for VM');
INSERT INTO "AUTH_GROUP" VALUES ('cd9ea5bf-7972-45c2-a0fd-b7e75fc2e5db', 'VmSfAdmin',    30,     5, 'Admin access to storage facility for VM');
INSERT INTO "AUTH_GROUP" VALUES ('b08efccf-a4a7-41b0-b208-acc8fc9c9bb4', 'TmuSfRead',    10,     6, 'Read access to storage facility for TMU');
INSERT INTO "AUTH_GROUP" VALUES ('4f8204c3-d48f-4a7c-9c79-1136edc337fb', 'TmuSfWrite',   20,     6, 'Write access to storage facility for TMU');
INSERT INTO "AUTH_GROUP" VALUES ('611202c5-8c69-444d-9b82-6aac5be150e4', 'TmuSfAdmin',   30,     6, 'Admin access to storage facility for TMU');
INSERT INTO "AUTH_GROUP" VALUES ('b1bd705b-a6b4-48ee-b517-f1c7be1bb015', 'KmnSfRead',    10,     7, 'Read access to storage facility for KMN');
INSERT INTO "AUTH_GROUP" VALUES ('c875ae85-7a01-4594-8e3e-7e4fbc792489', 'KmnSfWrite',   20,     7, 'Write access to storage facility for KMN');
INSERT INTO "AUTH_GROUP" VALUES ('53a87b63-4628-482a-b28c-40689129b962', 'KmnSfAdmin',   30,     7, 'Admin access to storage facility for KMN');


SET search_path = "MUSARK_STORAGE", pg_catalog;

INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('MoveObject');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('MovePlace');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('EnvRequirement');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('Control');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('Observation');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlAlcohol');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlCleaning');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlGas');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlHypoxicAir');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlLightingCondition');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlMold');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlPest');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlRelativeHumidity');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ControlTemperature');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationAlcohol');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationCleaning');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationFireProtection');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationGas');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationHypoxicAir');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationLightingCondition');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationMold');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationPerimeterSecurity');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationRelativeHumidity');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationPest');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationTemperature');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationTheftProtection');
INSERT INTO "EVENT_TYPE" ("NAME") VALUES ('ObservationWaterDamageAssessment');

INSERT INTO "ROLE" ("NAME", "DATA_TYPE") VALUES ('DoneWith', 'object');
INSERT INTO "ROLE" ("NAME", "DATA_TYPE") VALUES ('DoneBy', 'actor');
INSERT INTO "ROLE" ("NAME", "DATA_TYPE") VALUES ('toPlace', 'storageNode');
INSERT INTO "ROLE" ("NAME", "DATA_TYPE") VALUES ('fromPlace', 'storageNode');

-- ===========================================================================
-- Inserting test data
-- ===========================================================================

INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Utviklingsmuseet', NULL, NULL, '1', NULL, NULL, NULL, false, 'Root', NULL, NULL, ',1,', 99, '896125d3-0563-46b6-a7c5-51f3f899ff0a', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Utenfor museet', NULL, NULL, '1', NULL, NULL, NULL, false, 'Root', NULL, NULL, ',2,', 99, '896125d3-0563-46b6-a7c5-51f3f899ff0a', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Utviklingsmuseet Org', NULL, NULL, '1', 1, NULL, NULL, false, 'Organisation', NULL, NULL, ',1,3,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Forskningens hus', NULL, NULL, '1', 3, NULL, NULL, false, 'Building', NULL, NULL, ',1,3,4,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Kulturværelset', NULL, NULL, '1', 4, NULL, NULL, false, 'Room', NULL, NULL, ',1,3,4,5,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Naturværelset', NULL, NULL, '1', 4, NULL, NULL, false, 'Room', NULL, NULL, ',1,3,4,6,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Forskningsværelset', NULL, NULL, '1', 4, NULL, NULL, false, 'Room', NULL, NULL, ',1,3,4,7,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');

INSERT INTO "ORGANISATION" ("STORAGE_NODE_ID", "POSTAL_ADDRESS") VALUES (3, NULL);
INSERT INTO "BUILDING" ("STORAGE_NODE_ID", "POSTAL_ADDRESS") VALUES (4, NULL);
INSERT INTO "ROOM" ("STORAGE_NODE_ID", "PERIMETER_SECURITY", "THEFT_PROTECTION", "FIRE_PROTECTION", "WATER_DAMAGE_ASSESSMENT", "ROUTINES_AND_CONTINGENCY_PLAN", "RELATIVE_HUMIDITY", "TEMPERATURE_ASSESSMENT", "LIGHTING_CONDITION", "PREVENTIVE_CONSERVATION") VALUES (5, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO "ROOM" ("STORAGE_NODE_ID", "PERIMETER_SECURITY", "THEFT_PROTECTION", "FIRE_PROTECTION", "WATER_DAMAGE_ASSESSMENT", "ROUTINES_AND_CONTINGENCY_PLAN", "RELATIVE_HUMIDITY", "TEMPERATURE_ASSESSMENT", "LIGHTING_CONDITION", "PREVENTIVE_CONSERVATION") VALUES (6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO "ROOM" ("STORAGE_NODE_ID", "PERIMETER_SECURITY", "THEFT_PROTECTION", "FIRE_PROTECTION", "WATER_DAMAGE_ASSESSMENT", "ROUTINES_AND_CONTINGENCY_PLAN", "RELATIVE_HUMIDITY", "TEMPERATURE_ASSESSMENT", "LIGHTING_CONDITION", "PREVENTIVE_CONSERVATION") VALUES (7, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 5, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 5, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 5, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 5, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 6, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 6, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 6, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 6, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 6, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 7, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 7, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 7, 99);
INSERT INTO "LOCAL_OBJECT" ("LATEST_MOVE_ID", "CURRENT_LOCATION_ID", "MUSEUM_ID") VALUES (9999, 7, 99);

SET search_path = "MUSIT_MAPPING", pg_catalog;

-- Inserting default test users for the test museum
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Gjestebruker', '1a17c60c-755d-4d88-9449-12bbd5abe5e2', 99, '46e7d0fb-ca34-4603-b9c7-04aaaddffb72');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Normalbruker', '3655615b-e385-4833-b414-9957ea225e58', 99, '1cfa0dd9-2515-4726-9ae9-d592f7090d4c');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Super Duper User', '896125d3-0563-46b6-a7c5-51f3f899ff0a', 99, '9d7e4cdf-3a7a-47ed-aa4b-a70b847e7d7f');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Stein A. Olsen', '34e74d46-d45c-49dc-ace7-587b0ff40646', 99, '450f20cc-17c5-4d9f-8d78-fbd5acdcc84a');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Fake Musit Test User', '8efd41bb-bc58-4bbf-ac95-eea21ba9db81', 99, '5f5d3fc9-13b9-44c8-b4c6-fcee2d46b687');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Dawn Williams', 'dd457c4e-668e-41c5-8820-168f816a531b', 99, 'ea8a9ce3-69fb-43c0-b5e4-90f7ec4444ef');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Espen Uleberg', 'ca04b20e-5094-4b02-8b72-05ec629e7cb4', 99, '8fe8acbc-9391-453f-9973-c949d3323964');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Heini Emilia Rämä', '54c2e6f8-53f6-46be-b029-6f01b4b97231', 99, '967ff75d-5bac-49e2-8bb8-fe81713367e7');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Karstein Hårsaker', '71899f45-08da-48ef-b4c3-0f519d27157f', 99, 'b388e077-ace1-4d91-843e-7575fa1a24ab');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Marielle Bergh', '064c9dff-9956-4f4e-b3f7-ed77b6e5bc62', 99, 'f35f603a-1231-4b20-b4d6-234b992135a1');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Wenche Brun', 'e5bc8090-705d-40be-bcfa-2b351e7f2339', 99, 'acecdc06-ceb6-49f7-8d3d-1191c5bdbf04');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Susan Matland', '6eb49c1b-82c7-478b-85e4-9d69d5a50057', 99, 'd848b866-b6bb-4da1-ab2c-429f8bbbb9c7');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Kjell Gunnar Mobekk', '86626c72-2da2-4fa5-969b-8226a4141dcf', 99, '409aa92d-957f-4d9f-8192-2b1c34f0df10');
INSERT INTO "ACTOR" ("ACTORNAME", "DATAPORTEN_UUID", "MUSEUM_ID", "APPLICATION_UUID")
VALUES ('Line Arild Sjo', '4acad74e-eb4d-4632-be84-7ada5fef1074', 99, '1bd9e24d-d800-455e-820e-a93a1bc71ff3');



INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID", "MAINOBJECT_ID") VALUES (1,  'MusK58', '1',   'Rar dings', 58, 1, 99, 1);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID", "MAINOBJECT_ID") VALUES (2,  'MusK58', '2',   'Mansjettknapp', 58, 2, 99, 1);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID", "MAINOBJECT_ID") VALUES (3,  'MusK58', '3',   'Spenne', 58, 3, 99, 1);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID", "MAINOBJECT_ID") VALUES (4,  'MusK58', '4',   'Briller', 58, 4, 99, 1);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (5,  'MusN36', '1',   'Husflue', 36, 1, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (6,  'MusN45', NULL,  'Dyngebille', 45, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (7,  'MusN36', '2',   'Spyflue', 36, 2, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (8,  'MusN37', NULL,  'Bendelorm', 37, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (9,  'MusN28', NULL,  'Tusenben', 28, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (10, 'MusK400', NULL, 'Brynje', 400, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (11, 'MusN337', NULL,'Sommerfulgvinge', 337, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (12, 'MusN77', NULL, 'Lavastein', 77, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (13, 'MusK43', NULL, 'Krakk', 43, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (14, 'MusK23', NULL, 'Lite skaft av ben', 23, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (15, 'MusK24', 'a',  'Lendeklede', 24, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (16, 'MusK24', 'b',  'Kokekar', 24, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (17, 'MusN21', '2',  'Snilebille', 21, 2, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (18, 'MusN22', NULL, 'Fluesopp', 22, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (19, 'MusN20', NULL, 'Ukjent mygg', 20, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (20, 'MusK33', NULL, 'Bronsjespenne', 33, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (21, 'MusK34', 'a',  'Kniv', 34, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (22, 'MusK34', 'b',  'Spydspiss', 34, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (23, 'MusN31', '2',  'Vakker blomst', 31, 2, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (24, 'MusN32', NULL, 'Museskjelett', 32, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (25, 'MusN30', NULL, 'Merkelig fisk', 30, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (26, 'MusK313', NULL,'Leirkrukke', 313, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (27, 'MusK314', 'a', 'Flintavslag', 314, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (28, 'MusK314', 'b', 'Runeskrift', 314, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (29, 'MusN311', '2', 'Steinskrubb', 311, 2, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (30, 'MusN312', NULL,'Eggeskall av skjelden fulg', 312, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (31, 'MusN310', NULL,'Barkebille', 310, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (32, 'MusK503', NULL,'Medisintromme', 503, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (33, 'MusK504', 'a', 'Perle av stein', 504, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (34, 'MusK504', 'b', 'Smykkeanheng av rav', 504, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (35, 'MusN501', '2', 'Sikamygg', 501, 2, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (36, 'MusN502', NULL,'Markblomst', 502, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (37, 'MusN500', NULL,'Furukvist', 500, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (38, 'MusK73', NULL, 'Nikkelmynt', 73, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (39, 'MusK74', 'a',  'Krukke med hank', 74, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (40, 'MusK74', 'b',  'Lokk til krukke', 74, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (41, 'MusK71', NULL, 'Skje av fugleben', 71, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (42, 'MusK72', 'a',  'Hellerisning, del 1', 72, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (43, 'MusK72', 'b',  'Hellerisning, del 2', 72, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (44, 'MusN81', '2',  'Fiskeskinn av brosme', 81, 2, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (45, 'MusN81', '3',  'Fiskeskinn av lange', 81, 3, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (46, 'MusN82', NULL, 'Skjelett av flygefisk', 82, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (47, 'MusN80', NULL, 'Smygemaur', 80, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (48, 'MusN83', '2',  'Vaskebille', 83, 2, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (49, 'MusN87', NULL, 'Karpatplante', 87, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (50, 'MusK105', NULL,'Pilspiss', 105, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (51, 'MusN11', NULL, 'Solsikke', 11, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (52, 'MusN55', NULL, 'Pungdyrskjelett', 55, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (53, 'MusK113', NULL,'Fin bendings', 113, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (54, 'MusN13', NULL, 'Makrellsopp', 13, NULL, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (55, 'MusK108', NULL,'Skinnpung', 108, NULL, 99);