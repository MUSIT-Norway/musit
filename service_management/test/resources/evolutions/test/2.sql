# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSARK_LOAN;

CREATE TABLE MUSARK_LOAN.LOAN_EVENT (
  event_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  type_id         INTEGER      NOT NULL,
  event_date      TIMESTAMP    NOT NULL,
  registered_by   VARCHAR2(36) NOT NULL,
  registered_date TIMESTAMP    NOT NULL,
  museum_id       INTEGER      NOT NULL,
  part_of         NUMBER(20),
  object_uuid     VARCHAR2(36),
  external_ref    VARCHAR2(100),
  note            VARCHAR2(500),
  event_json      CLOB,
  PRIMARY KEY (event_id)
);

CREATE TABLE MUSARK_LOAN.ACTIVE_LOAN (
  active_loan_id  NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  museum_id       INTEGER      NOT NUll,
  object_uuid     VARCHAR2(36) NOT NULL UNIQUE,
  event_id        NUMBER(20)   NOT NULL,
  return_date     TIMESTAMP    NOT NULL,
  PRIMARY KEY (active_loan_id)
);

CREATE TABLE MUSARK_LOAN.LENT_OBJECT (
  lent_object_id  NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  event_id        NUMBER(20)   NOT NULL,
  object_uuid     VARCHAR2(36) NOT NULL,
  PRIMARY KEY (lent_object_id)
);