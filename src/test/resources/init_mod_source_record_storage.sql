CREATE SCHEMA diku_mod_source_record_storage;
CREATE TABLE diku_mod_source_record_storage.records_lb (id UUID);
CREATE TABLE diku_mod_source_record_storage.marc_records_lb (id UUID, content JSONB);
