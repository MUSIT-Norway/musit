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

CREATE SCHEMA IF NOT EXISTS MUSIT_MAPPING;

-- TODO: Refactor this to ACTOR
CREATE TABLE MUSIT_MAPPING.VIEW_ACTOR (
   NY_ID BIGINT(20) NOT NULL AUTO_INCREMENT,
   ACTORNAME VARCHAR(512),
   DATAPORTEN_ID VARCHAR2(50),
   PRIMARY KEY (NY_ID)
);

CREATE SCHEMA IF NOT EXISTS MUSARK_ACTOR;

CREATE TABLE MUSARK_ACTOR.ORGANIZATION (
  ID BIGINT(20) NOT NULL AUTO_INCREMENT,
  FN VARCHAR(255) NOT NULL,
  NICKNAME VARCHAR(255),
  TEL VARCHAR(20),
  WEB VARCHAR(255),
  PRIMARY KEY (ID)
);

CREATE TABLE MUSARK_ACTOR.ORGANIZATION_ADDRESS (
  ID BIGINT(20) NOT NULL AUTO_INCREMENT,
  ORGANIZATION_ID BIGINT(20) NOT NULL,
  TYPE VARCHAR(20),
  STREET_ADDRESS VARCHAR(20),
  LOCALITY VARCHAR(255),
  POSTAL_CODE VARCHAR(12),
  COUNTRY_NAME VARCHAR(255),
  LATITUDE FLOAT,
  LONGITUDE FLOAT,
  PRIMARY KEY (ID)
);

ALTER TABLE MUSARK_ACTOR.ORGANIZATION_ADDRESS ADD FOREIGN KEY (ORGANIZATION_ID) REFERENCES MUSARK_ACTOR.ORGANIZATION(ID);

INSERT INTO MUSIT_MAPPING.VIEW_ACTOR (ACTORNAME, DATAPORTEN_ID)
VALUES ('And, Arne1', '12345678-adb2-4b49-bce3-320ddfe6c90f');

INSERT INTO MUSIT_MAPPING.VIEW_ACTOR (ACTORNAME, DATAPORTEN_ID)
VALUES ('Kanin, Kalle1', 'd5a9a938-0905-470b-a892-88d0e7359579');

INSERT INTO MUSARK_ACTOR.ORGANIZATION (ID, FN, NICKNAME, TEL, WEB)
VALUES (1, 'Kulturhistorisk museum - Universitetet i Oslo', 'KHM', '22 85 19 00', 'www.khm.uio.no');

INSERT INTO MUSARK_ACTOR.ORGANIZATION_ADDRESS (ORGANIZATION_ID, TYPE, STREET_ADDRESS, LOCALITY, POSTAL_CODE, COUNTRY_NAME, LATITUDE, LONGITUDE)
VALUES (1, 'WORK', 'Fredriks gate 2', 'OSLO', '0255', 'NORWAY', 0.0, 0.0);

# --- !Downs

DROP TABLE MUSIT_MAPPING.VIEW_ACTOR;
DROP TABLE MUSARK_ACTOR.ORGANIZATION_ADDRESS;
DROP TABLE MUSARK_ACTOR.ORGANIZATION;