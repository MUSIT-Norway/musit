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


CREATE TABLE VIEW_ACTOR (
   NY_ID BIGINT(20) NOT NULL AUTO_INCREMENT,  # Should be ID
   ACTORNAME VARCHAR(512),                    # Should be FN
   PRIMARY KEY (NY_ID)
);

# CREATE TABLE PERSON (
#   ID BIGINT(20) NOT NULL AUTO_INCREMENT,
#   FN VARCHAR(255),
#   TITLE VARCHAR(255),
#   ROLE VARCHAR(255),
#   TEL VARCHAR(20),
#   EMAiL VARCHAR(255),
#   PRIMARY KEY (ID)
# );

CREATE TABLE ORGANIZATION (
  ID BIGINT(20) NOT NULL AUTO_INCREMENT,
  FN VARCHAR(255) NOT NULL,
  NICKNAME VARCHAR(255),
  TEL VARCHAR(20),
  WEB VARCHAR(255),
  LATITUDE FLOAT,
  LONGITUDE FLOAT,
  PRIMARY KEY (ID)
);

CREATE TABLE ORGANIZATION_ADDRESS (
  ID BIGINT(20) NOT NULL AUTO_INCREMENT,
  ORGANIZATION_ID BIGINT(20) NOT NULL,
  TYPE VARCHAR(20),
  STREET_ADDRESS VARCHAR(20),
  LOCALITY VARCHAR(255),
  POSTAL_CODE VARCHAR(12),
  COUNTRY_NAME VARCHAR(255),
  PRIMARY KEY (ID)
);
ALTER TABLE ORGANIZATION_ADDRESS ADD FOREIGN KEY (ORGANIZATION_ID) REFERENCES ORGANIZATION(ID);
 
insert into VIEW_ACTOR (actorname) values ('And, Arne1');
insert into VIEW_ACTOR (actorname) values ('Kanin, Kalle1');
#insert into PERSON (FN, TITLE, ROLE, TEL, EMAIL) values ('Klaus Myrseth', 'Løsnings arkitekt', 'System arkitekt', '93297177', 'klaus.myrseth@usit.uio.no')
insert into ORGANIZATION (ID, FN, NICKNAME, TEL, WEB, LATITUDE, LONGITUDE) values (1, 'Kulturhistorisk museum - Universitetet i Oslo', 'KHM', '22 85 19 00', 'www.khm.uio.no', null, null);
insert INTO ORGANIZATION_ADDRESS (ORGANIZATION_ID, TYPE, STREET_ADDRESS, LOCALITY, POSTAL_CODE, COUNTRY_NAME) values (1, "WORK", "Fredriks gate 2", "OSLO", "0255", "NORWAY");

# --- !Downs

DROP TABLE VIEW_ACTOR;
#DROP TABLE PERSON;
DROP TABLE ORGANIZATION_ADDRESS;
DROP TABLE ORGANIZATION;