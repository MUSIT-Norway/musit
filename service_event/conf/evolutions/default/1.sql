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


CREATE SCHEMA IF NOT EXISTS MUSARK_EVENT;


CREATE TABLE MUSARK_EVENT.EVENT_TYPE (
  ID INTEGER NOT NULL ,
  Name VARCHAR(100) NOT NULL,
  Description VARCHAR(255),
  PRIMARY KEY (ID)
);


CREATE TABLE MUSARK_EVENT.EVENT (
  ID BIGINT(20) NOT NULL AUTO_INCREMENT,
  EVENT_TYPE_ID integer not null, -- Move to separate table if we want to allow multiple instantiations
  NOTE VARCHAR2(4000),
  PRIMARY KEY (ID),
  FOREIGN KEY (EVENT_TYPE_ID) REFERENCES MUSARK_EVENT.EVENT_TYPE(ID)
);




insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (1,'Move');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (2,'Control');
insert into MUSARK_EVENT.EVENT_TYPE (id,Name) values (3,'Observation');

# --- !Downs

DROP TABLE MUSARK_EVENT.EVENT;
DROP TABLE MUSARK_EVENT.EVENT_TYPE
