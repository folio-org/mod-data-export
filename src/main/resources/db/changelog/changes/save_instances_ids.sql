CREATE OR REPLACE PROCEDURE save_instances_ids(jobExecutionId text, sqlWhere text)
LANGUAGE plpgsql
AS $$
DECLARE
  executeSql text;
BEGIN
 executeSql = 'insert into job_executions_export_ids(job_execution_id, instance_id) select ''' || jobExecutionId || ''' as job_execution_id, id as instance_id from v_instance ' || sqlWhere;
EXECUTE executeSql;
END;
$$;
