ALTER TABLE job_executions_export_ids
  ADD CONSTRAINT unique_export_id_for_job_execution UNIQUE(job_execution_id, instance_id);
