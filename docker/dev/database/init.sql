CREATE SCHEMA IF NOT EXISTS "MUSARK_STORAGE";

-- ===========================================================================
-- Tables for storage nodes
-- ===========================================================================

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
  PRIMARY KEY ("OBJECT_ID")
 );


-- ===========================================================================
-- Pre-populating necessary data
-- ===========================================================================

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
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Forskningens hus', NULL, NULL, '1', 2, NULL, NULL, false, 'Building', NULL, NULL, ',1,3,4,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Kulturværelset', NULL, NULL, '1', 3, NULL, NULL, false, 'Room', NULL, NULL, ',1,3,4,5,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Naturværelset', NULL, NULL, '1', 3, NULL, NULL, false, 'Room', NULL, NULL, ',1,3,4,6,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');
INSERT INTO "STORAGE_NODE" ("STORAGE_NODE_NAME", "AREA", "AREA_TO", "IS_STORAGE_UNIT", "IS_PART_OF", "HEIGHT", "HEIGHT_TO", "IS_DELETED", "STORAGE_TYPE", "GROUP_READ", "GROUP_WRITE", "NODE_PATH", "MUSEUM_ID", "UPDATED_BY", "UPDATED_DATE") VALUES ('Forskningsværelset', NULL, NULL, '1', 3, NULL, NULL, false, 'Room', NULL, NULL, ',1,3,4,7,', 99, 'd63ab290-2fab-42d2-9b57-2475dfbd0b3c', '2016-01-01 00:00:00');

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

-- INSERT INTO "VIEW_ACTOR" ("ACTORNAME", "DATAPORTEN_ID") VALUES ('Jarle Stabell', 'jarle');

INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (1,  'MusK58', '1',   'Rar dings', 58, 1, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (2,  'MusK58', '2',   'Mansjettknapp', 58, 2, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (3,  'MusK58', '3',   'Spenne', 58, 3, 99);
INSERT INTO "MUSITTHING" ("OBJECT_ID", "MUSEUMNO", "SUBNO", "TERM", "MUSEUMNOASNUMBER", "SUBNOASNUMBER", "MUSEUMID") VALUES (4,  'MusK58', '4',   'Briller', 58, 4, 99);
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