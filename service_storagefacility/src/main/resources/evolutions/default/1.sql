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
# Example schema

# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

CREATE TABLE MUSARK_STORAGE.STORAGE_UNIT(
 storage_unit_id   BIGINT NOT NULL  AUTO_INCREMENT,
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

-- Event tables

CREATE TABLE MUSARK_STORAGE.EVENT_TYPE (
  ID INTEGER NOT NULL ,
  Name VARCHAR(100) NOT NULL,
  Description VARCHAR(255),
  PRIMARY KEY (ID)
);


CREATE TABLE MUSARK_STORAGE.EVENT (
  ID BIGINT(20) NOT NULL AUTO_INCREMENT,
  EVENT_TYPE_ID integer not null, -- Move to separate table if we want to allow multiple instantiations
  NOTE VARCHAR2(4000),

  EVENT_DATE date, -- When the event happened

  REGISTERED_BY VARCHAR2(100),
  REGISTERED_DATE timestamp, -- could probably equivalently use datetime.

  VALUE_LONG long, -- Custom value, events can choose to store some event-specific value here.
  VALUE_String clob, -- Custom value, events can choose to store some event-specific value here.
  VALUE_FLOAT float, -- Custom value, events can choose to store some event-specific value here.

  PART_OF long,
  PRIMARY KEY (ID),
  FOREIGN KEY (EVENT_TYPE_ID) REFERENCES MUSARK_STORAGE.EVENT_TYPE(ID),
  FOREIGN KEY (PART_OF) REFERENCES MUSARK_STORAGE.EVENT(ID)
);


CREATE TABLE MUSARK_STORAGE.EVENT_RELATION_EVENT (
  FROM_EVENT_ID BIGINT(20) NOT NULL,
  RELATION_ID integer not null,
  TO_EVENT_ID BIGINT(20) NOT NULL,
  FOREIGN KEY (FROM_EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID),
  FOREIGN KEY (TO_EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID)
);

CREATE TABLE MUSARK_STORAGE.ACTOR_ROLE (
  ID Integer NOT NULL,
  NAME varchar2(200) NOT NULL,
  DESCRIPTION varchar2(200),
  PRIMARY KEY (ID)
);

insert into MUSARK_STORAGE.ACTOR_ROLE(ID, NAME, DESCRIPTION) VALUES (1, 'DoneBy', 'The actor who has executed/done the event');

CREATE TABLE MUSARK_STORAGE.EVENT_ROLE_ACTOR (
  EVENT_ID BIGINT(20) NOT NULL,
  ROLE_ID Integer NOT NULL,
  ACTOR_ID integer NOT NULL,
  PRIMARY KEY (EVENT_ID, ROLE_ID, ACTOR_ID),
  FOREIGN KEY (EVENT_ID) REFERENCES MUSARK_STORAGE.EVENT(ID),
  FOREIGN KEY (ROLE_ID) REFERENCES MUSARK_STORAGE.ACTOR_ROLE(ID)
  --Actor_ID is in another microservice so no foreign key allowed... :(
);


CREATE TABLE MUSARK_STORAGE.OBSERVATION_FROM_TO (
  ID BIGINT(20) NOT NULL,
  VALUE_FROM NUMBER,
  VALUE_TO NUMBER,
  PRIMARY KEY (ID),
  FOREIGN KEY (ID) REFERENCES MUSARK_STORAGE.EVENT(ID)
);

CREATE TABLE MUSARK_STORAGE.E_ENVIRONMENT_REQUIREMENT
(
 id         BIGINT(20) NOT NULL,
 temperature             NUMBER,
 temp_interval    NUMBER,
 air_humidity     NUMBER,
 air_hum_interval NUMBER,
 hypoxic_air      NUMBER,
 hyp_air_interval NUMBER,
 cleaning         VARCHAR2(250),
 light            VARCHAR2(250),
 PRIMARY KEY (ID),
   FOREIGN KEY (ID) REFERENCES MUSARK_STORAGE.EVENT(ID)
);


CREATE TABLE MUSARK_STORAGE.OBSERVATION_PEST_LIFECYCLE
(
 event_id         BIGINT(20) NOT NULL,
 stage       VARCHAR2(250),
 number             integer,
   FOREIGN KEY (event_id) REFERENCES MUSARK_STORAGE.EVENT(ID)
);

CREATE TABLE URI_LINKS (
      ID bigint(20) NOT NULL AUTO_INCREMENT,
      LOCAL_TABLE_ID bigint(20) NOT NULL,
      REL varchar(255) NOT NULL,
      HREF varchar(2000) NOT NULL,
      PRIMARY KEY (ID)
    );

insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (1, 'Move');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (2, 'EnvRequirement');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (3, 'Control');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (4, 'Observation');

insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (5, 'ControlAlcohol');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (6, 'ControlCleaning');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (7, 'ControlGas');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (8, 'ControlHypoxicAir');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (9, 'ControlLightingCondition');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (10, 'ControlMold');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (11, 'ControlPest');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (12, 'ControlRelativeHumidity');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (13, 'ControlTemperature');

insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (14, 'ObservationAlcohol');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (15, 'ObservationCleaning');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (16, 'ObservationFireProtection');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (17, 'ObservationGas');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (18, 'ObservationHypoxicAir');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (19, 'ObservationLightingCondition');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (20, 'ObservationMold');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (21, 'ObservationPerimeterSecurity');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (22, 'ObservationRelativeHumidity');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (23, 'ObservationPest');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (24, 'ObservationTemperature');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (25, 'ObservationTheftProtection');
insert into MUSARK_STORAGE.EVENT_TYPE (id,Name) values (26, 'ObservationWaterDamageAssessment');

# --- !Downs

DROP TABLE ROOM;
DROP TABLE BUILDING;
DROP TABLE STORAGE_UNIT_LINK;
DROP TABLE STORAGE_UNIT;
DROP TABLE EVENT;
DROP TABLE EVENT_TYPE;