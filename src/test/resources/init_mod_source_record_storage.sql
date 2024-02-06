CREATE SCHEMA diku_mod_source_record_storage;
CREATE SCHEMA diku_mod_entities_links;
CREATE TYPE diku_mod_source_record_storage.record_state AS ENUM ('ACTUAL', 'DRAFT', 'OLD', 'DELETED');
CREATE TYPE diku_mod_source_record_storage.record_type AS ENUM ('MARC_BIB', 'MARC_AUTHORITY', 'MARC_HOLDING', 'EDIFACT');
CREATE TABLE diku_mod_source_record_storage.records_lb (id UUID, external_id UUID, leader_record_status character(1),
  state diku_mod_source_record_storage.record_state, record_type diku_mod_source_record_storage.record_type, suppress_discovery boolean DEFAULT false);
CREATE TABLE diku_mod_source_record_storage.marc_records_lb (id UUID, content JSONB);
CREATE TABLE diku_mod_entities_links.authority (id UUID);
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('17eed93e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "17eed93e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "4a090b0f-9da3-40f1-ab17-33d6a1e3abae"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type)
	VALUES ('17eed93e-f9e2-4cb2-a52b-e9155acfc119', '4a090b0f-9da3-40f1-ab17-33d6a1e3abae', 'c', 'ACTUAL', 'MARC_AUTHORITY');
INSERT INTO diku_mod_entities_links.authority (id)
  VALUES ('4a090b0f-9da3-40f1-ab17-33d6a1e3abae');

CREATE SCHEMA central_mod_source_record_storage;
CREATE TYPE central_mod_source_record_storage.record_state AS ENUM ('ACTUAL', 'DRAFT', 'OLD', 'DELETED');
CREATE TYPE central_mod_source_record_storage.record_type AS ENUM ('MARC_BIB', 'MARC_AUTHORITY', 'MARC_HOLDING', 'EDIFACT');
CREATE TABLE central_mod_source_record_storage.records_lb (id UUID, external_id UUID, leader_record_status character(1),
  state diku_mod_source_record_storage.record_state, record_type diku_mod_source_record_storage.record_type, suppress_discovery boolean DEFAULT false);
CREATE TABLE central_mod_source_record_storage.marc_records_lb (id UUID, content JSONB);
INSERT INTO central_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('ed0ad74c-98f1-11ee-b9d1-0242ac120002', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "ed0ad74c-98f1-11ee-b9d1-0242ac120002"
                                                               },
                                                               {
                                                                 "i": "26be956e-98f2-11ee-b9d1-0242ac120002"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO central_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type)
	VALUES ('ed0ad74c-98f1-11ee-b9d1-0242ac120002', '26be956e-98f2-11ee-b9d1-0242ac120002', 'c', 'ACTUAL', 'MARC_AUTHORITY');
