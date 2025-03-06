ALTER TABLE job_execution_export_files ALTER COLUMN status TYPE VARCHAR(128) USING status::TEXT;
ALTER TABLE job_executions ALTER COLUMN status TYPE VARCHAR(128) USING status::TEXT;

DROP TYPE IF EXISTS executionstatustype CASCADE;
DROP TYPE IF EXISTS statustype CASCADE;
