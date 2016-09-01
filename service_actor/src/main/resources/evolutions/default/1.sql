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

CREATE TABLE MUSIT_MAPPING.VIEW_ACTOR (
   NY_ID BIGINT(20) NOT NULL AUTO_INCREMENT,
   ACTORNAME VARCHAR(512),
   PRIMARY KEY (NY_ID)
);

CREATE SCHEMA IF NOT EXISTS MUSARK_ACTOR;

CREATE TABLE MUSARK_ACTOR.PERSON (
  ID BIGINT(20) NOT NULL AUTO_INCREMENT,
  FN VARCHAR(255),
  TITLE VARCHAR(255),
  ROLE VARCHAR(255),
  TEL VARCHAR(20),
  EMAIL VARCHAR(255),
  WEB VARCHAR(255),
  DATAPORTEN_ID varchar2(50),
  PRIMARY KEY (ID)
);

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
 
insert into MUSIT_MAPPING.VIEW_ACTOR (actorname) values ('And, Arne1');
insert into MUSIT_MAPPING.VIEW_ACTOR (actorname) values ('Kanin, Kalle1');
insert into MUSARK_ACTOR.PERSON (FN, TITLE, ROLE, TEL, EMAIL, WEB, DATAPORTEN_ID) values ('Klaus Myrseth', 'Løsnings arkitekt', 'System arkitekt', '93297177', 'klaus.myrseth@usit.uio.no', 'vg.no', '12345678-adb2-4b49-bce3-320ddfe6c90f');
insert into MUSARK_ACTOR.ORGANIZATION (ID, FN, NICKNAME, TEL, WEB) values (1, 'Kulturhistorisk museum - Universitetet i Oslo', 'KHM', '22 85 19 00', 'www.khm.uio.no');
insert into MUSARK_ACTOR.ORGANIZATION_ADDRESS (ORGANIZATION_ID, TYPE, STREET_ADDRESS, LOCALITY, POSTAL_CODE, COUNTRY_NAME, LATITUDE, LONGITUDE) values (1, 'WORK', 'Fredriks gate 2', 'OSLO', '0255', 'NORWAY', 0.0, 0.0);

# --- !Downs

DROP TABLE MUSIT_MAPPING.VIEW_ACTOR;
DROP TABLE MUSARK_ACTOR.PERSON;
DROP TABLE MUSARK_ACTOR.ORGANIZATION_ADDRESS;
DROP TABLE MUSARK_ACTOR.ORGANIZATION;