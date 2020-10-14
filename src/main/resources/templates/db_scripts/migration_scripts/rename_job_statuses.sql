-- update jobs with no exported records, set status 'FAIL'
UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb, '{status}', '"FAIL"')
WHERE jsonb -> 'progress' ->> 'exported' = '0' OR jsonb ->> 'status' = 'FAIL';

-- update jobs with all the exported records, set status 'COMPLETED'
UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb, '{status}', '"COMPLETED"')
WHERE jsonb -> 'progress' ->> 'failed' = '0' and
	    jsonb -> 'progress' ->> 'exported' > '0' and jsonb ->> 'status' = 'SUCCESS';

-- update jobs with partially failed records, set status 'COMPLETED_WITH_ERRORS'
UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb, '{status}', '"COMPLETED_WITH_ERRORS"')
WHERE jsonb -> 'progress' ->> 'failed' > '0' and
      jsonb -> 'progress' ->> 'exported' > '0';
