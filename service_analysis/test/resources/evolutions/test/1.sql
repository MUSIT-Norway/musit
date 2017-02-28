# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSARK_ANALYSIS;

-- CREATE SEQUENCE MUSARK_ANALYSIS.EVENT_TYPE_SEQ
-- START WITH 1
-- INCREMENT BY 1
-- NOCACHE;
--
-- CREATE TABLE MUSARK_ANALYSIS.EVENT_TYPE (
--   id            INTEGER MUSARK_ANALYSIS.EVENT_TYPE_SEQ.NEXTVAL,
--   event_type_id VARCHAR2(36)  NOT NULL, -- UUID
--   name          VARCHAR2(100) NOT NULL,
--   short_name    VARCHAR2(50),
--   attributes    CLOB,
--   PRIMARY KEY (event_type_id)
-- );

CREATE TABLE MUSARK_ANALYSIS.EVENT_TYPE (
  id            INTEGER GENERATED BY DEFAULT AS IDENTITY,
  event_type_id VARCHAR2(36)  NOT NULL, -- UUID
  category      INTEGER       NOT NULL,
  name          VARCHAR2(100) NOT NULL,
  short_name    VARCHAR2(50),
  attributes    CLOB,
  PRIMARY KEY (event_type_id)
);

CREATE TABLE MUSARK_ANALYSIS.EVENT (
  event_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  event_type_id   VARCHAR2(36) NOT NULL,
  event_date      TIMESTAMP    NOT NULL,
  registered_by   VARCHAR2(36) NOT NULL,
  registered_date TIMESTAMP    NOT NULL, -- When the event was received by the system
  note            VARCHAR2(500),
  event_json      CLOB,
  PRIMARY KEY (event_id)
);

CREATE TABLE MUSARK_ANALYSIS.RESULT (
  result_id   NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  event_id    NUMBER(20) NOT NULL,
  -- TODO: What else should be part of the result table?
  result_json CLOB,
  PRIMARY KEY (result_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_ANALYSIS.EVENT (event_id)
);

-- INSERTING EVENT_TYPE DATA

INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (event_type_id, name, short_name, attributes) VALUES ('uuid', 'name', 'sname', 'attr JSON')
