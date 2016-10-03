# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSIT_MAPPING;

CREATE TABLE MUSIT_MAPPING.VIEW_MUSITTHING (
 id BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
 displayId VARCHAR NOT NULL,
 displayName VARCHAR
);

CREATE SCHEMA IF NOT EXISTS MUSARK_STORAGE;

CREATE TABLE MUSARK_STORAGE.LOCAL_OBJECT (
 object_id BIGINT(20) NOT NULL,
 latest_move_id BIGINT(20),
 current_location_id INTEGER,
 FOREIGN KEY(object_id) REFERENCES MUSIT_MAPPING.VIEW_MUSITTHING(id)
);

CREATE TABLE MUSARK_STORAGE.STORAGE_NODE (
 storage_node_id BIGINT NOT NULL AUTO_INCREMENT,
 PRIMARY KEY (storage_node_id)
);
