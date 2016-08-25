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

# --- !Downs


DROP TABLE ROOM;
DROP TABLE BUILDING;
DROP TABLE STORAGE_UNIT_LINK;
DROP TABLE STORAGE_UNIT;