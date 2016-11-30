# --MUSARK_AUTH schema

# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSARK_AUTH;

CREATE TABLE MUSARK_AUTH.AUTH_GROUP (
  group_uuid VARCHAR2(36) NOT NULL,
  group_name VARCHAR(100) NOT NULL,
  group_permission INTEGER NOT NULL,
  group_museumId INTEGER NOT NULL,
  group_description VARCHAR(512),
  PRIMARY KEY (group_uuid),
  CONSTRAINT unique_group_name UNIQUE (group_name)
);

CREATE TABLE MUSARK_AUTH.USER_AUTH_GROUP (
  user_feide_email VARCHAR(254) NOT NULL,
  group_uuid VARCHAR2(36) NOT NULL,
  collection_uuid VARCHAR(36),
  PRIMARY KEY (user_feide_email, group_uuid),
  FOREIGN KEY (group_uuid) REFERENCES MUSARK_AUTH.AUTH_GROUP (group_uuid)
--   ,
--   FOREIGN KEY (collection_uuid) REFERENCES MUSARK_AUTH.MUSEUM_COLLECTION (collection_uuid)
);

CREATE TABLE MUSARK_AUTH.MUSEUM_COLLECTION (
  collection_uuid VARCHAR2(36) NOT NULL,
  collection_name VARCHAR(100),
  collection_schema_identifiers VARCHAR(100) NOT NULL,
  PRIMARY KEY (collection_uuid)
);

CREATE TABLE MUSARK_AUTH.USER_INFO (
  user_uuid VARCHAR2(36) NOT NULL,
  secondary_id VARCHAR(512),
  name VARCHAR(512),
  email VARCHAR(254),
  picture VARCHAR(100),
  PRIMARY KEY (user_uuid)
);


# --- !Downs
