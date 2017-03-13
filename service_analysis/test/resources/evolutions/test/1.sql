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


CREATE TABLE MUSARK_ANALYSIS.SAMPLE_OBJECT (
  sample_uuid          VARCHAR2(36)      NOT NULL,
  parent_object_uuid   VARCHAR2(36),
  is_collection_object INTEGER DEFAULT 0 NOT NULL,
  museum_id            INTEGER           NOT NULL,
  status               INTEGER DEFAULT 1 NOT NULL,
  responsible_actor_id VARCHAR2(36)      NOT NULL,
  created_date         TIMESTAMP         NOT NULL,
  sample_id            VARCHAR2(100),
  external_id          VARCHAR2(100),
  note                 VARCHAR2(250),
  registered_by   VARCHAR2(36) NOT NULL,
  registered_date TIMESTAMP    NOT NULL,
  updated_by           VARCHAR2(36),
  updated_date         TIMESTAMP,
  PRIMARY KEY (sample_uuid)
);

-- Keeps track of which analysis'/objects are sent to a lab in batch
-- CREATE TABLE MUSARK_ANALYSIS.SHIPMENT (
--   shipment_uuid VARCHAR2(36)   NOT NULL,
--   to_actor      VARCHAR2(36)   NOT NULL,
--   description   VARCHAR2(200)  NOT NULL,
--   items         VARCHAR2(1000) NOT NULL,
--   PRIMARY KEY (shipment_uuid)
-- );

CREATE TABLE MUSARK_ANALYSIS.EVENT_TYPE (
  type_id     VARCHAR2(36)  NOT NULL,
  category    INTEGER       NOT NULL,
  name        VARCHAR2(100) NOT NULL,
  short_name  VARCHAR2(50),
  collections VARCHAR2(500), -- if empty then all collections, else value is ',uuid_1,uuid_2,uuid_9,'     LIKE '%,uuid_2,%'
  attributes  CLOB,
  PRIMARY KEY (type_id)
);

CREATE TABLE MUSARK_ANALYSIS.EVENT (
  event_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  type_id         VARCHAR2(36) NOT NULL,
  event_date      TIMESTAMP    NOT NULL,
  registered_by   VARCHAR2(36) NOT NULL,
  registered_date TIMESTAMP    NOT NULL,
  part_of         NUMBER(20),
  object_uuid     VARCHAR2(36),
  note            VARCHAR2(500),
  event_json      CLOB,
  PRIMARY KEY (event_id)
);

CREATE TABLE MUSARK_ANALYSIS.RESULT (
  result_id       NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  event_id        NUMBER(20)   NOT NULL,
  -- result_uuid     VARCHAR2(36) NOT NULL, -- This identifies 1 of many results belonging to an event_id
  registered_by   VARCHAR2(36) NOT NULL,
  registered_date TIMESTAMP    NOT NULL,
  -- TODO: What else should be part of the result table?
  result_json     CLOB,
  PRIMARY KEY (result_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_ANALYSIS.EVENT (event_id)
);

-- INSERTING EVENT_TYPE DATA
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('fabe6462-ea94-43ce-bf7f-724a4191e114', 2, 'C/N-ratio', 'C/N', NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b15ee459-38c9-414f-8b54-7c6439b44d3d', 1, 'Telling av vekstsoner/årringer', NULL, '{"age":"String"}', ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('55fdf44b-f4dc-45d1-bdb6-ee6e745123a0', 2, 'LA-ICP-MS (sporelementer)', 'La-ICP-MS', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('80a4f3e1-5b45-433f-83c0-8a388364beba', 2, 'Pyrolyse gasskromatografi-massespektrometri', 'Py-GC/MS', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('38c1d623-4005-4562-aa57-0efbeee2837d', 2, 'Atomabsorbsjonspektroskopi', 'AAS', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('7214d962-cd0f-492e-b353-d0bafce6ecb7', 2, 'Electron Micro Probe analysis', 'EMPA', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('639f5dd9-e115-4907-8c84-f4b2de9bf9e7', 2, 'Electron spin resonance', 'ESR/EPR', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('1ac71fb5-20c3-4df3-8331-45fbb3b52c76', 2, 'Energidispersiv røntgenspektroskopi', 'EDS/EDX', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('7d29027f-13e4-46d3-82a6-329ae31e21e1', 2, 'Fouriertransform infrarød spektroskopi', 'FTIR', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('48b4b466-a91c-4314-9e6d-570aa506478b', 2, 'Gasskromatografi', 'GC', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('fcf7a176-dddd-4410-8b9c-22c3fc888d67', 2, 'Gasskromatografi-massespektrometri', 'GC-MS', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('eddaaefb-18be-4539-b9a5-81b53a66d6c7', 2, 'GC-combustion-isotope ratio-MS', 'GC-C-IRMS', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b6f13ca5-cae7-4eb2-ac29-34c4044bce34', 2, 'Induktivt koblet plasma massespektrometri', 'ICP-MS', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b0caabfb-eb1d-4a7a-97c8-8fd7ce0527c2', 2, 'Induktivt koblet plasma optisk emisjonsspektroskopi', 'ICP-OES, ICP-AES', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('93db4c4a-6a42-4463-94ac-0b1bb064d5d4', 2, 'Ioneselektiv elektrode, klorid', 'ISE', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('4d786c49-8564-4cb5-8168-fecd9cb2a9a6', 2, 'Ioneselektiv elektrode, pH', 'ISE', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('537974c5-5c41-47f7-874b-50e211c3cf34', 2, 'Massespektrometri', 'MS', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('6103b028-a167-4227-b468-bcfb0ec0ea74', 2, 'Miljøgifter, organiske og uorganske komponeneter', 'GC/MS, LC/MS, GC/ECD, etc.', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('9bb688df-5556-4b25-80d2-676c025be34a', 2, 'Multi-angle light scattering', 'MALS', NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('9bfd8e52-8c30-4680-9bee-3640911218c6', 2, 'Neutron imaging (tomography)', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('8d14aa8c-ec1f-498f-8eea-a8a07c15b773', 2, 'Nuclear magnetic resonance', 'NMR', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('c02b5575-3f33-4a28-b603-979043ddf108', 2, 'Nøytron aktiveringsanalyse', 'NAA', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('4f1ae652-13f0-462f-8514-87f951578ce3', 2, 'PIXE (Particle induced X-ray emission spectroscopy)', 'PIXE', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('33740494-9a05-4b76-bd80-2d46d190bb70', 2, 'Raman-spektroskopi', 'Raman', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('6a86e5d8-e084-480e-b50a-b17f48d3903b', 2, 'Røntgendiffraksjon', 'XRD', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('52568da0-fad9-478b-aa70-ac2a96ec9e30', 2, 'Røntgenfluorescensspektroskopi', 'XRF', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('818284da-ad3c-4293-a9ff-a0c7a294b1fb', 2, 'Tungmetaller, uorganisk', 'GFAAS, CVAAS, AFS, ICP-AES, ICP-MS, ICP-SFMS ', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('86dffc43-f3df-4445-a265-abd5fb18e88a', 2, 'UV-visible light spektroskopi', NULL, NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('6ec27bf6-8ecc-4664-9b1a-fcd00a1430c1', 2, 'Væskekromatografi', 'HPLC', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('398af141-09f7-43b9-98e0-84dfc2d5f73a', 2, 'X-ray Absorption Near Edge Structure', 'XANES', NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('9db59531-3d8d-48dd-83c5-4a78fdc24246', 3, 'Flowcytometri', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b07e0e0a-a8c8-4ac6-b2e4-4a8876790913', 4, 'Kolorimeter (spektrofotometer)', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('898b2cad-7996-4b03-99ca-a4e0af7c5ed7', 4, 'Reflektans spektroradiometri', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('93dce6cc-7779-4a4f-b4b4-cb7fda6c7021', 5, 'Dendrokronologi', 'Dendro', '{"age":"String"}', ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('2ee9d36b-8097-4009-a632-ce5b48af09b4', 2, 'Forbrenningsteknikk', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('8453873d-227c-4205-a231-bf7e04164fab', 5, 'Geokronologi: Ar/Ar datering', 'Ar/Ar', '{"age":"String"}', NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('46e92edd-3a3a-40a6-9064-c3f7a0d27537', 5, 'Geokronologi: K-Ar datering', 'K-Ar', '{"age":"String"}', NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('05c4d60f-6282-4ca9-8563-1486440ad577', 5, 'Geokronologi: O18/O16', 'O18/O16', '{"age":"String"}', NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b64f77d3-8430-4dd1-a41e-d094a800255e', 5, 'Geokronologi: U-Pb datering', 'U-Pb', '{"age":"String"}', NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('3ba66794-9425-4432-a859-558b111313c3', 5, 'Optisk stimulerende luminesens', 'OSL', '{"age":"String"}', ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('5ed64f56-c759-4244-aaaa-4720599457aa', 5, 'Radiokarbondatering', 'C14', '{"age":"String"}', ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('5ab56169-632a-493e-a9cf-dc68b2c7d625', 5, 'Termoluminesens', 'TL', '{"age":"String"}', ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('d1419727-0510-46ba-adac-8e427d990504', 6, 'Barkoding', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('22f15907-122f-4d72-97e5-f8d0b27e1bed', 6, 'NGS-sekvensering', 'NGS', NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('5ae83a0e-2514-4189-953a-92cac40c987f', 6, 'Sanger-sekvensering', 'Sanger', NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('4201b094-6a0c-4e02-b5bf-85462a135efd', 6, 'Genetisk kjønnsbestemmelse', NULL, NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('cfdcc1a1-35ce-42c2-a743-4970d81380e6', 6, 'DNA-analyse', 'DNA', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('c641fa89-cf10-4724-a707-f8048c7ea773', 6, 'Mikrosatellitt-analyse', 'MSAT', NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('aba1fefa-e137-4328-9188-d2ef7f89cda9', 6, 'Ekstraksjon (DNA, RNA, ...)', NULL, NULL, NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('fed18a86-5887-4cd9-a95e-3c4b2298e8ba', 7, 'Petrofysiske og paleomagnetiske målinger', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('5507325e-9584-46f1-8cf9-a43fa0599d6e', 7, 'Petrografi (tynnslip) og mineralogi, se også PLM', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('1bbf15cb-8348-4e66-99a4-bc314da57a42', 8, '3D-skanning, laser', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b39399ab-aabd-4ebc-903b-adcf6876a364', 8, '3D-skanning, strukturert lys', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('0a1e1346-69ad-468c-8075-a6707571f2fb', 8, 'Komputertomografi', 'CT', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('9a8fec08-a7d3-42bd-814d-0965d4c97888', 8, 'Røntgenavbilding (tomografi)', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('9be7bd59-b3ed-4beb-821c-3cc76fb2f1ea', 8, 'Røntgenfotografering', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('acfffa46-b092-44a8-9f9f-e27cdbb2aea9', 8, 'Røntgenmikroskopi', 'XRM', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('5fe5608b-5d75-4f35-b9ee-c498f4ec39a7', 8, 'Skanning elektronmikroskopi', 'SEM-EDS', NULL, NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('48d33719-4189-4969-9d21-7ca6f87926c5', 8, 'Transmisjon elektronmikroskopi', 'TEM', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b7120224-50b0-48cd-bbd0-bb33ab66e831', 8, 'Ultrafiolett fluorisering', 'UV', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('24748725-9a05-43f4-bc2b-6bdaf642da74', 9, 'Isotopanalyse, karbon', '13C/12C', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b61a4b54-3dc8-4002-90b1-049e440903c5', 9, 'Isotopanalyse, nitrogen', '15N/14N', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('2c4476be-8a75-490d-8d7b-8c77fd552458', 9, 'Isotopanalyse, oksygen', '18O/16O', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('4320233a-a609-41d7-acf4-10038c07a452', 9, 'Isotopanalyse, svovel', '34S/32S', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('34c53969-883f-47ba-bec9-e9bd0f4846ca', 9, 'Isotopanalyser, bly', '210Pb', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('0b8e820f-2ed8-4202-b5f0-1f3725ac118f', 9, 'Isotopanalyser, hydrogen', '2H/1H', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('3856ae35-d886-479f-95a7-30c252a5a3da', 9, 'Isotopanalyser, strontium', '87Sr/86Sr', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('54b8ac3f-376e-4eb8-b610-d41fd631bb7c', 9, 'Isotopanalyser, strontium/neodymium', 'Sr/Nd', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('cc0bc699-baaa-4c68-b8a1-b651717057c9', 10, 'Glødetapsanalyse', 'LOI', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('2ba63b93-827f-495c-83f8-4dec0362aed4', 11, 'frø, frukter, div. planterester', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('4becce96-0cf1-47d7-ab41-0f398eb4bd1f', 11, 'Foraminiferer (poredyr)', NULL, NULL, NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('2627e80e-c112-4be2-ad3e-4fd1bf80f5e7', 11, 'Mollusker, snegler og skjell', NULL, NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('917682f4-32bc-45c8-a5c8-e929bcdb1aff', 11, 'Palaeoentomologi', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('f4ea40da-629c-441f-8895-01d62c7f161a', 12, 'Mikroskopisk trekull', 'Antrax', NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('f60c15a7-66e7-40f6-a51b-eace174b1ad0', 12, 'Algeanalyse', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('ed2cea38-2ea2-4340-b4ee-4c2be1837657', 12, 'Diatomeanalyse (kiselalger)', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('048f6932-3926-43e0-90a2-7fb7f9485c71', 12, 'Fytolitter (stivelseskorn)', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('59145616-3f95-477a-bad9-654dd1461d43', 12, 'Pollen-og sporeanalyse', 'P', NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('c31d580a-4eda-4628-97fe-e0f573f6ee17', 12, 'Dinoflaggelatanalyse', NULL, NULL, NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('1c0c1990-65b6-4ed1-b9b8-b7c097d0d88f', 12, 'Non pollen palynomorfer, inkl. soppsporer', 'NPP', NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('55e280c9-fbe4-419e-ac16-79ed7560f83c', 13, 'Metallografi', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('e0c8deb9-b840-40e6-8bac-263e3ba28fe6', 13, 'Sperm morfologi', NULL, NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('abc711ff-9ad5-4921-a00e-1f8762f1e637', 13, 'Målinger, tellinger m.m.', NULL, NULL, NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('882755a6-dc2f-448c-b0e6-fdfe8c74dc69', 13, 'Sporemålinger', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('5bbf9ca8-7293-4767-8d6b-03f6d60aff60', 14, 'Skjelettanalyse, dyrebein', NULL, NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('fbbece62-891d-476f-b5ae-5bec0bbe99be', 14, 'Skjelettanalyse, menneske', NULL, NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('1ed31a2f-2f32-44cf-950a-dbd42f2a4313', 15, 'Enzymelektroforese', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('79661550-0bda-4406-be5c-43e1104647f3', 15, 'Isoenzymanalyse', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('d6515e89-2fbe-4346-be6f-7fc9980095bd', 15, 'Proteinanalyse', NULL, NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('a88b249c-6839-46e0-b06c-91889a6529a7', 16, 'Geotekniske analyser', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('34094c6b-b65a-407c-aa4b-1d7cfe097488', 16, 'Kornfordelingsanalyse med Coulter laser partikkelteller', NULL, NULL, NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('1b587755-0cfa-48ca-8626-a13912a43c14', 16, 'Ledningsevne for jordprøver etter oppslemming i vann', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('a25f1d48-22df-4bb1-bb4c-75753cd1c3ca', 16, 'Mineralseparasjon', NULL, NULL, NULL);
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('231a565a-15cc-4dcd-93e4-4922bc8d328e', 16, 'Tephraanalyse (microprobe)', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('bc901e06-b517-44d4-8f15-86e3ecd70e12', 16, 'Fosfatanalyse', 'Cit-P', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('a16da6e1-a2f8-4cc3-bdce-115315958027', 16, 'Jordmikromorfologi', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('2ce40edc-d9df-4e18-8d4a-ae2f3faeac02', 16, 'Kornfordelingsanalyse: Tørr-/våtsikting', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('5f06f6be-52ed-47db-9683-d66a8319f259', 16, 'Magnetisk suseptibilitet', 'MSc', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('ae57018f-1bc0-4beb-ba42-1c68ccd1b283', 16, 'Teksturanalyse (rundingsanalyse, kornform)', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('b82d06f1-b56a-4cdb-ae34-145fd88fe1ff', 17, 'Fargereaksjoner', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('58235176-5795-4cb9-9071-87ec638ede27', 17, 'Lysreaksjoner', NULL, NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('5efba732-c6d6-4386-acb8-79df0c51144a', 17, 'Tynnsjiktkromatografi', 'TLC', NULL, ',fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('13c77021-9d2a-4636-966c-14d211dc3ae5', 18, 'Tekstil- og fiberanalyse', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('139e022d-1232-4337-8865-fe6e3d6d1ee9', 18, 'Tynnsjiktkromatografi', 'TLC', NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('dfba46d6-875a-48c6-a431-a246e8db6df9', 19, 'Slitesporanalyse', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('e7907040-49d5-4ae5-bb30-4c5f6cf4f956', 20, 'Analyse av sperm motilitet vha. CASA', 'CASA', NULL, ',ba3d4d30-810b-4c07-81b3-37751f2196f0,ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4,');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (type_id, category, name, short_name, attributes, collections) VALUES ('75a8ce01-04a2-4e1c-8f23-e87fe4fbf0f8', 21, 'forkullet/uforkullet ved', NULL, NULL, ',2e4f2455-1b3b-4a04-80a1-ba92715ff613,88b35138-24b5-4e62-bae4-de80fae7df82,8bbdf9b3-56d1-479a-9509-2ea82842e8f8,fcb4c598-8b05-4095-ac00-ce66247be38a,d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24,23ca0166-5f9e-44c2-ab0d-b4cdd704af07,1d8dd4e6-1527-439c-ac86-fc315e0ce852,7352794d-4973-447b-b84e-2635cafe910a,');