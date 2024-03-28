CREATE SEQUENCE IF NOT EXISTS job_execution_hrId MINVALUE 1 NO MAXVALUE CACHE 1 NO CYCLE;

CREATE TABLE IF NOT EXISTS file_definitions (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    creation_date TIMESTAMP,
    created_by TEXT
);

DROP TRIGGER IF EXISTS set_id_in_jsonb ON file_definitions;
DROP TRIGGER IF EXISTS set_file_definitions_md_trigger ON file_definitions;
DROP TRIGGER IF EXISTS set_file_definitions_md_json_trigger ON file_definitions;

CREATE TABLE IF NOT EXISTS mapping_profiles (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    creation_date TIMESTAMP,
    created_by TEXT
);

DROP TRIGGER IF EXISTS set_id_in_jsonb ON mapping_profiles;
DROP TRIGGER IF EXISTS set_mapping_profiles_md_trigger ON mapping_profiles;
DROP TRIGGER IF EXISTS set_mapping_profiles_md_json_trigger ON mapping_profiles;

CREATE TABLE IF NOT EXISTS job_profiles (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    creation_date TIMESTAMP,
    created_by TEXT,
    mappingprofileid uuid,
    constraint mappingprofileid_mapping_profiles_fkey foreign key (mappingprofileid)
      references mapping_profiles(id)
      ON UPDATE NO ACTION
      ON DELETE NO ACTION
);

DROP TRIGGER IF EXISTS set_id_in_jsonb ON job_profiles;
DROP TRIGGER IF EXISTS update_job_profiles_references ON job_profiles;
DROP TRIGGER IF EXISTS set_job_profiles_md_trigger ON job_profiles;
DROP TRIGGER IF EXISTS set_job_profiles_md_json_trigger ON job_profiles;

CREATE TABLE IF NOT EXISTS job_executions (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    jobprofileid uuid,
    constraint jobprofileid_job_profiles_fkey foreign key (jobprofileid)
      references job_profiles(id)
      ON UPDATE NO ACTION
      ON DELETE NO ACTION
);

DROP TRIGGER IF EXISTS set_id_in_jsonb ON job_executions;
DROP TRIGGER IF EXISTS update_job_executions_references ON job_executions;

CREATE TABLE IF NOT EXISTS error_logs (
    id uuid PRIMARY KEY,
    jsonb jsonb,
    creation_date TIMESTAMP,
    created_by TEXT,
    jobprofileid uuid,
    constraint fk_error_log_to_job_profile foreign key (jobprofileid)
      references job_profiles(id)
      ON UPDATE NO ACTION
      ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS job_executions_export_ids (
     id bigint GENERATED ALWAYS AS IDENTITY,
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

CREATE OR REPLACE VIEW v_instance_hrid
    AS SELECT id, jsonb->>'hrid' as hrid FROM ${myuniversity}_mod_inventory_storage.instance;

CREATE OR REPLACE VIEW v_holdings_record
    AS SELECT id, jsonb, instanceid as instance_id FROM ${myuniversity}_mod_inventory_storage.holdings_record;

CREATE OR REPLACE VIEW v_item
    AS SELECT id, jsonb, holdingsrecordid as holdings_record_id FROM ${myuniversity}_mod_inventory_storage.item;

CREATE OR REPLACE VIEW v_marc_records_lb
    AS SELECT id, content, external_id, record_type::text, state::text, leader_record_status, suppress_discovery, generation
    FROM ${myuniversity}_mod_source_record_storage.records_lb records_lb
    JOIN ${myuniversity}_mod_source_record_storage.marc_records_lb using(id);
