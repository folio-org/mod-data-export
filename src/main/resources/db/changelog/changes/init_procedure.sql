CREATE OR REPLACE PROCEDURE save_instances_ids(jobExecutionId text, sqlWhere text)
LANGUAGE plpgsql
AS $$
DECLARE
  executeSql text;
BEGIN
 executeSql = 'INSERT INTO job_executions_export_ids(job_execution_id, instance_id) SELECT ''' || jobExecutionId || ''' AS job_execution_id, id AS instance_id FROM v_instance ' || sqlWhere;
EXECUTE executeSql;
END;
$$;
