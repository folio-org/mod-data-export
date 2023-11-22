CREATE SEQUENCE IF NOT EXISTS job_execution_hrId MINVALUE 1 NO MAXVALUE CACHE 1 NO CYCLE;

CREATE TABLE IF NOT EXISTS file_definitions (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    creation_date TIMESTAMP,
    created_by TEXT
);

CREATE TABLE IF NOT EXISTS mapping_profiles (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    creation_date TIMESTAMP,
    created_by TEXT,
    name TEXT
);

CREATE TABLE IF NOT EXISTS job_profiles (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    creation_date TIMESTAMP,
    created_by TEXT,
    mapping_profile_id uuid,
    name TEXT,
    constraint fk_job_profile_to_mapping_profile foreign key (mapping_profile_id)
        references mapping_profiles(id) ON DELETE CASCADE
);

CREATE TYPE ExecutionStatusType AS ENUM ('NEW', 'IN_PROGRESS', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAIL');
CREATE CAST (character varying as ExecutionStatusType) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS job_executions (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    job_profile_id uuid,
    status ExecutionStatusType,
    completed_date TIMESTAMP,
    constraint fk_job_execution_to_job_profile foreign key (job_profile_id)
        references job_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS error_logs (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    creation_date TIMESTAMP,
    created_by TEXT,
    job_profile_id uuid,
    constraint fk_error_log_to_job_profile foreign key (job_profile_id)
        references job_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS job_executions_export_ids (
     id int GENERATED ALWAYS AS IDENTITY,
     job_execution_id uuid,
     instance_id uuid,
     constraint fk_export_id_to_job_execution foreign key (job_execution_id)
         references job_executions(id) ON DELETE CASCADE
);

CREATE TYPE StatusType AS ENUM ('SCHEDULED', 'ACTIVE', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED');
CREATE CAST (character varying as StatusType) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS job_execution_export_files (
    id uuid PRIMARY KEY,
    job_execution_id uuid,
    file_location TEXT,
    from_id uuid,
    to_id uuid,
    status StatusType,
    constraint fk_export_files_to_job_execution foreign key (job_execution_id)
          references job_executions(id) ON DELETE CASCADE
);

CREATE OR REPLACE VIEW v_instance
    AS SELECT id, jsonb FROM ${myuniversity}_mod_inventory_storage.instance;

CREATE OR REPLACE VIEW v_holdings_record
    AS SELECT id, jsonb, instanceid as instance_id FROM ${myuniversity}_mod_inventory_storage.holdings_record;

CREATE OR REPLACE VIEW v_item
    AS SELECT id, jsonb, holdingsrecordid as holdings_record_id FROM ${myuniversity}_mod_inventory_storage.item;

CREATE OR REPLACE VIEW v_authority
    AS SELECT * FROM ${myuniversity}_mod_inventory_storage.authority;

CREATE OR REPLACE VIEW v_marc_records_lb
    AS SELECT id, content, external_id FROM ${myuniversity}_mod_source_record_storage.records_lb
    JOIN ${myuniversity}_mod_source_record_storage.marc_records_lb using(id);
