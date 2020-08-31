-- update jobs with no exported records, set status 'FAILED'
UPDATE diku_mod_data_export.job_executions
SET jsonb = jsonb_set(jsonb, '{status}', '"FAILED"')
WHERE jsonb -> 'progress' ->> 'exported' = '0';

-- update jobs with all the exported records, set status 'COMPLETED'
UPDATE diku_mod_data_export.job_executions
SET jsonb = jsonb_set(jsonb, '{status}', '"COMPLETED"')
WHERE jsonb -> 'progress' ->> 'failed' = '0' and
	    jsonb -> 'progress' ->> 'exported' > '0';

-- update jobs with partially failed records, set status 'COMPLETED_WITH_ERRORS'
UPDATE diku_mod_data_export.job_executions
SET jsonb = jsonb_set(jsonb, '{status}', '"COMPLETED_WITH_ERRORS"')
WHERE jsonb -> 'progress' ->> 'failed' > '0' and
      jsonb -> 'progress' ->> 'exported' > '0';
