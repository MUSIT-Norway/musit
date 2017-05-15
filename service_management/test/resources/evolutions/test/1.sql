# --- !Ups

CREATE SCHEMA IF NOT EXISTS MUSARK_ANALYSIS;
CREATE SCHEMA IF NOT EXISTS MUSARK_LOAN;

CREATE TABLE MUSARK_ANALYSIS.SAMPLE_OBJECT (
  sample_uuid            VARCHAR2(36)      NOT NULL,
  parent_object_uuid     VARCHAR2(36),
  parent_object_type     VARCHAR(50),
  is_extracted           INTEGER DEFAULT 0 NOT NULL,
  museum_id              INTEGER           NOT NULL,
  status                 INTEGER DEFAULT 1 NOT NULL,
  responsible_actor      VARCHAR2(512),
  done_date              TIMESTAMP,
  sample_id              VARCHAR2(100),
  sample_num             INTEGER AUTO_INCREMENT,
  external_id            VARCHAR2(100),
  external_id_source     VARCHAR2(100),
  sample_type_id         INTEGER,
  sample_size            NUMBER,
  sample_size_unit       VARCHAR2(10),
  sample_container       VARCHAR2(100),
  storage_medium         VARCHAR2(100),
  treatment              VARCHAR2(100),
  leftover_sample        INTEGER DEFAULT 1 NOT NULL,
  description            VARCHAR2(250),
  note                   VARCHAR2(250),
  originated_object_uuid VARCHAR2(36)      NOT NULL,
  registered_by          VARCHAR2(36)      NOT NULL,
  registered_date        TIMESTAMP         NOT NULL,
  updated_by             VARCHAR2(36),
  updated_date           TIMESTAMP,
  is_deleted             INTEGER DEFAULT 0 NOT NULL,
  PRIMARY KEY (sample_uuid)
);

CREATE TABLE MUSARK_ANALYSIS.SAMPLE_TYPE (
  sampletype_id    NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  no_sampletype    VARCHAR2(100) NOT NULL,
  en_sampletype    VARCHAR2(100) NOT NULL,
  no_samplesubtype VARCHAR2(100),
  en_samplesubtype VARCHAR2(100),
  PRIMARY KEY (sampletype_id)
);

CREATE TABLE MUSARK_ANALYSIS.EVENT_TYPE (
  type_id                      INTEGER GENERATED BY DEFAULT AS IDENTITY,
  category                     INTEGER       NOT NULL,
  no_name                      VARCHAR2(100) NOT NULL,
  en_name                      VARCHAR2(100) NOT NULL,
  short_name                   VARCHAR2(50),
  collections                  VARCHAR2(500), -- if empty then all collections, else value is ',uuid_1,uuid_2,uuid_9,'     LIKE '%,uuid_2,%'
  extra_description_type       VARCHAR2(50),
  extra_description_attributes CLOB,
  extra_result_attributes      CLOB,
  PRIMARY KEY (type_id)
);

CREATE TABLE MUSARK_ANALYSIS.EVENT (
  event_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  type_id         INTEGER NOT NULL,
  done_by         VARCHAR2(512),
  done_date       TIMESTAMP,
  registered_by   VARCHAR2(36) NOT NULL,
  registered_date TIMESTAMP    NOT NULL,
  part_of         NUMBER(20),
  object_uuid     VARCHAR2(36),
  note            VARCHAR2(500),
  status          INTEGER,
  case_numbers    VARCHAR2(1000),
  event_json      CLOB,
  PRIMARY KEY (event_id)
);

CREATE TABLE MUSARK_ANALYSIS.RESULT (
  event_id        NUMBER(20)   NOT NULL,
  registered_by   VARCHAR2(36) NOT NULL,
  registered_date TIMESTAMP    NOT NULL,
  result_json     CLOB,
  PRIMARY KEY (event_id),
  FOREIGN KEY (event_id) REFERENCES MUSARK_ANALYSIS.EVENT (event_id)
);

CREATE TABLE MUSARK_ANALYSIS.TREATMENT (
  treatment_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  no_treatment VARCHAR2(100) NOT NULL,
  en_treatment VARCHAR2(100) NOT NULL,
  PRIMARY KEY (treatment_id)
);

CREATE TABLE MUSARK_ANALYSIS.STORAGECONTAINER (
  storagecontainer_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  no_storagecontainer VARCHAR2(100) NOT NULL,
  en_storagecontainer VARCHAR2(100) NOT NULL,
  PRIMARY KEY (storagecontainer_id)
);

CREATE TABLE MUSARK_ANALYSIS.STORAGEMEDIUM (
  storagemedium_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  no_storagemedium VARCHAR2(100) NOT NULL,
  en_storagemedium VARCHAR2(100) NOT NULL,
  PRIMARY KEY (storagemedium_id)
);

CREATE TABLE MUSARK_LOAN.LOAN_EVENT (
  event_id        NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  type_id         INTEGER      NOT NULL,
  event_date      TIMESTAMP    NOT NULL,
  registered_by   VARCHAR2(36) NOT NULL,
  registered_date TIMESTAMP    NOT NULL,
  museum_id       INTEGER      NOT NULL,
  part_of         NUMBER(20),
  object_uuid     VARCHAR2(36),
  case_numbers    VARCHAR2(1000),
  note            VARCHAR2(500),
  event_json      CLOB,
  PRIMARY KEY (event_id)
);

CREATE TABLE MUSARK_LOAN.ACTIVE_LOAN (
  active_loan_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  museum_id      INTEGER      NOT NULL,
  object_uuid    VARCHAR2(36) NOT NULL UNIQUE,
  event_id       NUMBER(20)   NOT NULL,
  return_date    TIMESTAMP    NOT NULL,
  PRIMARY KEY (active_loan_id)
);

CREATE TABLE MUSARK_LOAN.LENT_OBJECT (
  lent_object_id NUMBER(20) GENERATED BY DEFAULT AS IDENTITY,
  event_id       NUMBER(20)   NOT NULL,
  object_uuid    VARCHAR2(36) NOT NULL,
  PRIMARY KEY (lent_object_id)
);

-- INSERTING EVENT_TYPE DATA
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (11, 'HPLC', NULL, NULL, NULL, 'Væskekromatografi', 'High performance liquid chromatography');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (11, NULL, NULL, NULL, NULL, 'Fargereaksjon', 'Spot test');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (11, NULL, NULL, NULL, NULL, 'UV-lys', 'UV-light');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (11, 'TLC', NULL, NULL, NULL, 'Tynnsjiktkromatografi', 'Thin Layer Chromotography');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (5, NULL, 'MicroscopyAttributes', '{"method":"Int"}', NULL, 'Mikroskopi', 'Microscopy');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (5, NULL, NULL, NULL, NULL, 'Røntgenfotografering', 'Radiography');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (5, NULL, 'TomographyAttributes', '{"method":"Int"}', NULL, 'Tomografi', 'Tomography');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (5, 'UV', NULL, NULL, NULL, 'Ultrafiolett fluorisering', 'Ultraviolet fluorescence');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (3, 'C14', NULL, NULL, '{"ageEstimate":"String","standardDeviation":"String"}', 'Radiokarbondatering', 'Radiocarbon dating');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (3, 'Dendro', NULL, NULL, '{"age":"String"}', 'Dendrokronologi', 'Dendrochronology');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (2, NULL, NULL, NULL, NULL, 'Kolorimetri', 'Colorimetry');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (2, NULL, NULL, NULL, NULL, 'Spektrofotometri', 'Spectrophotometry');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (4, 'DNA', NULL, NULL, NULL, 'DNA-analyse (kulturhistorisk)', 'DNA analysis (cultural history)');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (4, NULL, NULL, NULL, NULL, 'Strekkoding', 'Barcoding');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (4, NULL, 'ExtractionAttributes', '{"extractionType":"String","method":"String"}', '{"storageMedium":"String","concentration":"Size","volume":"String"}', 'Ekstraksjon (DNA, RNA, ...)', 'Extraction (DNA, RNA, ..)');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (4, NULL, NULL, NULL, NULL, 'Mikrosatellitter', 'Microsatellites');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (4, 'NGS', NULL, NULL, NULL, 'NGS-sekvensering', 'NGS sequencing');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (4, NULL, NULL, NULL, NULL, 'Sanger-sekvensering', 'Sanger sequensing');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'GC-MS', NULL, NULL, NULL, 'Gasskromatografi-massespektrometri', 'Gas Chromatography - Mass Spectrometry');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'XRF', NULL, NULL, NULL, 'Røntgenfluorescensspektroskopi', 'X-ray fluorescence');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'MS', NULL, NULL, NULL, 'Massespektrometri', 'Mass spectrometry');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'SEM-EDX/EDS', NULL, NULL, NULL, 'Skanning elektron mikroskopi-Energidispersiv røntgenspektroskopi', 'Scanning electron microscopy - Energy-dispersive X-ray spectroscopy');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'FTIR', NULL, NULL, NULL, 'Fouriertransform infrarød spektroskopi', 'Fourier transform infrared spectroscopy');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'ISE-Cl', NULL, NULL, NULL, 'Ioneselektiv elektrode, klorid', 'Ion-selective electrode, chloride');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'ISE-H', NULL, NULL, NULL, 'Ioneselektiv elektrode, pH', 'Ion-selective electrode, pH');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'Py-GC/MS', NULL, NULL, NULL, 'Pyrolyse gasskromatografi-massespektrometri', 'Pyrolysis-gas chromatography/mass spectrometry');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'Raman', NULL, NULL, NULL, 'Raman-spektroskopi', 'Raman spectroscopy');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'XRD', NULL, NULL, NULL, 'Røntgendiffraksjon', 'X-ray diffraction');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'HPLC', NULL, NULL, NULL, 'Væskekromatografi', 'High performance liquid chromatography');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, NULL, 'IsotopeAttributes', '{"types":"Array[Int]"}', NULL, 'Isotopanalyse', 'Isotope analysis');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'NMR', NULL, NULL, NULL, 'Nuclear magnetic resonance spectroscopy', 'Nuclear magnetic resonance spectroscopy');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, NULL, NULL, NULL, NULL, 'Fettsyreanalyse', 'Lipids');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'XANES', NULL, NULL, NULL, 'X-ray Absorption Near Edge Structure', 'X-ray absorption near edge structure');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'ICP', 'ElementalICPAttributes', '{"method":"Int"}', NULL, 'Grunnstoffanalyse (ICP: Inductively coupled plasma analyses with different types of detectors)', 'Elemental analysis (ICP: Inductively coupled plasma analyses with different types of detectors)');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'AAS', 'ElementalAASAttributes', '{"method":"Int"}', NULL, 'Grunnstoffanalyse (AAS: atomabsorbsjonspektroskopi)', 'Elemental analysis (AAS: atomabsorbsjonspektroskopi)');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (1, 'AFS', NULL, NULL, NULL, 'Grunnstoffanalyse (AFS)', 'Elemental analysis (AFS)');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (6, NULL, NULL, NULL, NULL, 'Plantemateriale, makro (forkullet/uforkullet)', 'Plant material (charred/not charred)');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (7, 'P+S', NULL, NULL, NULL, 'Pollen og sporer', 'Pollen and spores');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (8, NULL, NULL, NULL, '{"measurementType":"String","size":"Size","precision":"String","method":"String"}', 'Målinger & tellinger', 'Counts & measurements');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (9, NULL, NULL, NULL, NULL, 'Skjelettanalyse, dyrebein', 'Osteology, non-human');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (9, NULL, NULL, NULL, NULL, 'Skjelettanalyse, menneske', 'Osteology, human');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (10, 'Cit-P', NULL, NULL, NULL, 'Fosfat', 'Phosphate determination');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (10, NULL, NULL, NULL, NULL, 'Jordmikromorfologi', 'Soil micromorphology');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (12, NULL, NULL, NULL, NULL, 'Tekstil- og fiberanalyse', 'Textile and fibre analysis');
INSERT INTO MUSARK_ANALYSIS.EVENT_TYPE (category, short_name, extra_description_type, extra_description_attributes, extra_result_attributes, no_name, en_name) VALUES (13, NULL, NULL, NULL, NULL, 'Vedanatomi (forkullet/uforkullet)', 'Wood anatomy (charred/uncharred)');

--- INSERTING TREATMENT-LIST
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (1, 'CTAB', 'CTAB');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (2, 'DNAdvance Beckman Coulter', 'Advance Beckman Coulter');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (3, 'EZNA plant kit', 'EZNA plant kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (4, 'Hot shot', 'Hot shot');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (5, 'Mole robot', 'Mole robot');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (6, 'Omega EZNA blood and tissue kit', 'Omega EZNA blood and tissue kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (7, 'Omega EZNA blood kit', ' Omega EZNA blood kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (8, 'Omega EZNA plant kit', ' Omega EZNA plant kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (9, 'Omega EZNA tissue kit', 'Omega EZNA tissue kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (10, 'Phenol-Chloroform', 'Phenol-Chloroform');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (11, 'Qiagen blood and tissue kit', 'Qiagen blood and tissue kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (12, 'Qiagen blood kit', 'Qiagen blood kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (13, 'Qiagen plant kit', 'Qiagen plant kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (14, 'Qiagen tissue kit', 'Qiagen tissue kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (15, 'Quick extract', 'Quick extract');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (16, 'WGA (GenomiPhi)', 'WGA (GenomiPhi)');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (17, 'Spectrum plant toal RNA kit', ' Spectrum plant toal RNA kit');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (18, 'Drilling', 'Drilling');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (19, 'Ekstrahering', 'Extraction');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (20, 'Fjernet ved bruk av skalpell', 'Removed using scalpel');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (21, 'Løse fragment(er)', 'Loose fragment(s)');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (22, 'Skjæring/kutting', 'Cutting');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (23, 'Skraping', 'Scraping');
INSERT INTO MUSARK_ANALYSIS.TREATMENT (treatment_id, no_treatment, en_treatment) VALUES (24, 'Saging', 'Sawing');


INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (1, 'Eppendorfrør', 'Eppendorf tube');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (2, 'Kryorør', 'Cryo tube');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (3, 'Lynlåspose', 'Ziplock bag');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (4, 'Lynlåspose, plast (LDPE)', 'Self-sealing bags, plastic (LDPE)');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (5, 'Mikrofilterplate', 'Microfilter plate');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (6, 'Mikrosentrifugerør', 'Microcentrifuge tube');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (7, 'Papirpose', 'Paper bag');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (8, 'PCR-rør', 'PCR tube');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (9, 'Større plastbeholder m skrulokk', 'Larger plastic container with screw lid');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (10, 'Større glassbeholder m skrulokk', 'Larger glas container with screw lid');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (11, 'Plastrør 0,5 mL', 'Plastic tube 0.2 ml');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (12, 'Plastrør 0,5 mL', 'Plastic tube 0.5 ml');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (13, 'Plastrør 1,5 mL', 'Plastic tube 1.5 ml');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (14, 'Plastrør 2,0 mL', 'Plastic tube 2.0 ml');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (15, 'Plastrør 8,0 mL', 'Plastic tube 8.0 ml');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (16, 'Plastrør 15 mL', 'Plastic tube 15 ml');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (17, 'Plastrør 50 mL', 'Plastic tube 50 ml');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (18, 'Plate 96 brønner', 'Plate 96 wells');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (19, 'Syrefri pappeske', 'Acid free cardboard box');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (20, 'Syrefritt silkepapir', 'Acid free tissue');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (21, 'Pappeske, ikke arkivstandard', 'Cardboard box, not archival standard');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (22, 'Polyetylen skum (Ethafoam/Plastazote)', 'Polyethylene Foam (Ethafoam/Plastazote)');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (23, 'Polystyren eske', 'Polystyrene box');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (24, 'Reagensglass', 'Locked test tubes');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (25, 'Prøvekube', 'Small locked test tubes');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (26, 'PVC-rør', 'PVC-tube');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (27, 'Kasseprøve', 'Wooden box');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER (storagecontainer_id, no_storagecontainer, en_storagecontainer) VALUES (28, 'Plastboks (5 L)', 'Box (plastic) 5 L');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER(storagecontainer_id, no_storagecontainer,en_storagecontainer) VALUES(29,'Isbiter boks','Ice-cube box');
INSERT INTO MUSARK_ANALYSIS.STORAGECONTAINER(storagecontainer_id, no_storagecontainer,en_storagecontainer) VALUES(30,'Preparateske for slides','Slidebox');


--- INSERTING SAMPLE_TYPES

INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(1,'DNA-ekstrakt','DNA extract','aDNA','aDNA');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(2,'DNA-ekstrakt','DNA extract','eDNA','eDNA');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(3,'DNA-ekstrakt','DNA extract','gDNA','gDNA');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(4,'DNA-ekstrakt','DNA extract','rDNA','rDNA');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(5,'Vev','Tissue','Frø','Seed');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(6,'Levende individ','Living individual','','');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(7,'Parasitt','Parasite','Ektoparasitt','Ectoparasite');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(8,'Parasitt','Parasite','Endoparasitt','Endoparasite');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(9,'RNA-bibliotek','RNA library','','');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(10,'RNA-ekstrakt','RNA extract','','');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(11,'Vev','Tissue','Apothecia','Apothecia');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(12,'Vev','Tissue','Bein','Bone');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(13,'Vev','Tissue','Binde- og støttevev','Connective tissue');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(14,'Vev','Tissue','Blad','Leaf');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(15,'Vev','Tissue','Blod','Blood');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(16,'Vev','Tissue','Epitelvev','Epithelial tissue');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(17,'Vev','Tissue','Fjær','Feather');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(18,'Vev','Tissue','Føtter','Legs');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(19,'Vev','Tissue','Grunnvev','Ground tissue');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(20,'Vev','Tissue','Hale','Tail');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(21,'Vev','Tissue','Hjerne','Brain');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(22,'Vev','Tissue','Hudvev','Epidermis');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(23,'Vev','Tissue','Hår','Hair');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(24,'Vev','Tissue','Knokkel','Bone');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(25,'Vev','Tissue','Ledningsvev','Vascular tissue');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(26,'Vev','Tissue','Lever','Liver');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(27,'Vev','Tissue','Muskel','Muscle');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(28,'Vev','Tissue','Nervevev','Nervous tissue');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(29,'Vev','Tissue','Plasma','Plasma');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(30,'Vev','Tissue','Skinn','Skin');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(31,'Vev','Tissue','Soredia','Soredia');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(32,'Vev','Tissue','Testikkel','Testicle');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(33,'Vev','Tissue','Thallus','Thallus');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(34,'Vev','Tissue','Tåpute','Toe pad');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(35,'Vev','Tissue','Vinge','Wing');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(36,'Vev','Tissue','Øre','Ear');
INSERT INTO MUSARK_ANALYSIS.SAMPLE_TYPE(sampletype_id,no_sampletype,en_sampletype,no_samplesubtype,en_samplesubtype)VALUES(37,'Materiale','Material','','');

INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (1, 'Lagringsmedium', 'Storage medium');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (2, 'Buffer EDTA', 'Buffer EDTA');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (3, 'Buffer TE ', 'Buffer TE');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (4, 'Buffer uspesifisert', 'Buffer (Unspecified)');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (5, 'Destillert vann', 'Distilled water');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (6, 'DMSO', 'DMSO');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (7, 'dsH20', 'dsH20');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (8, 'Elution buffer', 'Elution buffer');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (9, 'Etanol', 'Ethanol');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (10, 'Etanol 70%', 'Ethanol 70%');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (11, 'Etanol 80%', 'Ethanol 80%');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (12, 'Etanol 96%', 'Ethanol 96%');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (13, 'Fenol', 'Phenol');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (14, 'Flytende nitrogen', 'Liquid nitrogen');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (15, 'Formalin', 'Formalin');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (16, 'Glyserol', 'Glycerol');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (17, 'Fysiologisk saltvann', 'Physiological saline');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (18, 'Iseddik', 'Glacial acetic acid');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (19, 'Isopropanol', 'Isopropanol');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (20, 'Langmeir solution', 'Langmeir solution');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (21, 'Queens lysis buffer', 'Queens lysis buffer');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (22, 'RNAlater', 'RNAlater');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (23, 'SET buffer', 'SET buffer');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (24, 'Silika', 'Silica gel');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (25, 'Tørr', 'Dry');
INSERT INTO MUSARK_ANALYSIS.STORAGEMEDIUM (storagemedium_id, no_storagemedium, en_storagemedium) VALUES (26, 'Parafinvoks', 'Paraffin wax');