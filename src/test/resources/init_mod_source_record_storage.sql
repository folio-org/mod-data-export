CREATE SCHEMA diku_mod_source_record_storage;
CREATE TABLE diku_mod_source_record_storage.records_lb (id UUID, external_id UUID);
CREATE TYPE diku_mod_source_record_storage.recordType AS ENUM ('MARC_HOLDING');
CREATE TABLE diku_mod_source_record_storage.marc_records_lb (id UUID, content JSONB, record_type diku_mod_source_record_storage.recordType);
