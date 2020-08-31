-- update jobs with exported records, set status 'COMPLETED'
UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb, '{status}', '"COMPLETED"')
WHERE jsonb ->> 'status' = 'SUCCESS' and jsonb -> 'progress' -> 'failed' = '0';

-- update jobs with partially failed records, set status 'COMPLETED_WITH_ERRORS'
UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb, '{status}', '"COMPLETED_WITH_ERRORS"')
WHERE jsonb ->> 'status' = 'SUCCESS' and jsonb -> 'progress' -> 'failed' > '0' and jsonb -> 'progress' -> 'failed' < jsonb -> 'progress' -> 'total';
