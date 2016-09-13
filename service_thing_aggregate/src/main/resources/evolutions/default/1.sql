# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSIT_MAPPING;

CREATE TABLE MUSIT_MAPPING.VIEW_MUSITTHING (
 id BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
 displayId varchar NOT NULL,
 displayName varchar
);

CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT
(
 object_id         BIGINT(20) NOT NULL,
 latest_move_id      BIGINT(20),
 current_location_id  integer,
 FOREIGN KEY(object_id) REFERENCES MUSIT_MAPPING.VIEW_MUSITTHING(id)
);

CREATE TABLE MUSARK_STORAGE.STORAGE_NODE(
 storage_node_id BIGINT NOT NULL  AUTO_INCREMENT,
 primary key (storage_node_id)
);

insert INTO MUSARK_STORAGE.STORAGE_NODE(storage_node_id) VALUES(3);

insert into MUSIT_MAPPING.VIEW_MUSITTHING(id, displayId, displayName) values(1, 'C666/34', 'Øks');
insert into MUSIT_MAPPING.VIEW_MUSITTHING(id, displayId, displayName) values(2, 'C666/31', 'Sverd');
insert into MUSIT_MAPPING.VIEW_MUSITTHING(id, displayId, displayName) values(3, 'C666/38', 'Sommerfugl');

insert into MUSARK_STORAGE.LOCAL_OBJECT(object_id, latest_move_id, current_location_id) VALUES(1, 23, 3);
insert into MUSARK_STORAGE.LOCAL_OBJECT(object_id, latest_move_id, current_location_id) VALUES(2, 23, 3);
insert into MUSARK_STORAGE.LOCAL_OBJECT(object_id, latest_move_id, current_location_id) VALUES(3, 23, 3);