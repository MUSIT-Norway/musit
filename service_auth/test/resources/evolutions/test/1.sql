# --MUSARK_AUTH schema

# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSARK_AUTH;

CREATE TABLE MUSARK_AUTH.AUTH_GROUP (
  group_uuid VARCHAR2(36) NOT NULL,
  group_name VARCHAR(100) NOT NULL,
  group_permission INTEGER NOT NULL,
  group_description VARCHAR(512),
  PRIMARY KEY (group_uuid),
  CONSTRAINT unique_group_uuid UNIQUE (group_uuid)
);

CREATE TABLE MUSARK_AUTH.USER_AUTH_GROUP (
  user_uuid VARCHAR2(36) NOT NULL, -- Dataporten user UUID
  group_uuid VARCHAR2(36) NOT NULL,
  PRIMARY KEY (user_uuid, group_uuid),
  FOREIGN KEY (group_uuid) REFERENCES MUSARK_AUTH.AUTH_GROUP (group_uuid),
  CONSTRAINT unique_usergroup_uuids UNIQUE (user_uuid, group_uuid)
);


# --- !Downs
