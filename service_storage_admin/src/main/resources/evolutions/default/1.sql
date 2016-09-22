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

CREATE TABLE MUSARK_STORAGE.STORAGE_NODE(
 storage_node_id   BIGINT NOT NULL  AUTO_INCREMENT,
 storage_node_name VARCHAR(512),
 area              FLOAT,
 area_to           FLOAT,
 height            FLOAT,
 height_to         FLOAT,
 is_storage_unit   VARCHAR(1) DEFAULT '1', -- TODO: Remove, because it is redundant due to storage_type?
 is_part_of        INTEGER,
 parent_path       VARCHAR(1000) not null, -- Comma separated list of ids of the ancestors for this node. Leftmost is the root node. "," for root nodes.
 is_deleted        integer not null default 0,
 storage_type      varchar(100) default 'storageunit',
 group_read        varchar(4000),
 group_write       varchar(4000),
 latest_move_id    BIGINT,
 latest_envreq_id BIGINT,
primary key (storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.ROOM(
 storage_node_id             BIGINT not null,
 perimeter_security        integer,-- not null default 0,
 theft_protection       integer,-- not null default 1,
 fire_protection        integer,-- not null default 0,
 water_damage_assessment     integer,-- not null default 0,
 routines_and_contingency_plan integer,-- not null default 0,
 RELATIVE_HUMIDITY           integer,-- not null default 0,
 TEMPERATURE_ASSESSMENT      integer,-- not null default 0,
 lighting_condition            integer,-- not null default 0,
 preventive_conservation          integer,-- not null default 0,
 PRIMARY KEY (storage_node_id),
 FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE(storage_node_id)
 );

CREATE TABLE MUSARK_STORAGE.BUILDING(
 storage_node_id INTEGER not null ,
 postal_address  VARCHAR(512),
PRIMARY KEY (storage_node_id),
FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE(storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.ORGANISATION(
 storage_node_id INTEGER not null ,
 postal_address  VARCHAR(512),
PRIMARY KEY (storage_node_id),
FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE(storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.STORAGE_UNIT_LINK(
 link_id         BIGINT not null,
 storage_node_id BIGINT not null,
 link            VARCHAR(255) not null,
 relation        VARCHAR(100) not null,
 PRIMARY KEY (link_id),
 FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE(storage_node_id)
);

CREATE TABLE MUSARK_STORAGE.E_ENVIRONMENT_REQUIREMENT
(
 id             BIGINT(20) NOT NULL AUTO_INCREMENT,
 temperature      NUMBER,
 TEMPERATURE_TOLERANCE    NUMBER,
 RELATIVE_HUMIDITY     NUMBER,
 REL_HUM_TOLERANCE NUMBER,
 hypoxic_air      NUMBER,
 HYP_AIR_TOLERANCE NUMBER,
 cleaning         VARCHAR2(250),
 LIGHTING_COND            VARCHAR2(250),
 NOTE VARCHAR2(4000),
 --STORAGE_NODE_ID BIGINT NOT NULL,
  PRIMARY KEY (ID)
  --FOREIGN KEY (storage_node_id) REFERENCES MUSARK_STORAGE.STORAGE_NODE(storage_node_id)
);

--Copied from Event (will be in the merged microservice). Included here to be able to get some tests to run.
--CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT
--(
 --object_id         BIGINT(20) NOT NULL,
 --latest_move_id      BIGINT(20) ,
 --current_location_id  integer, -- maybe for later use
--   FOREIGN KEY (latest_move_id) REFERENCES MUSARK_EVENT.EVENT(ID)
   --FOREIGN KEY (current_location_id) REFERENCES MUSARK_EVENT.storageAdminNodehvatever(ID)
--);

# --- !Downs


DROP TABLE ROOM;
DROP TABLE BUILDING;
DROP TABLE ORGANISATION;
DROP TABLE STORAGE_UNIT_LINK;
DROP TABLE STORAGE_NODE;