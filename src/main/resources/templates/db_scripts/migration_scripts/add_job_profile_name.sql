UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb , '{jobProfileName}', '"default"')
WHERE jsonb -> 'jobProfileName' IS NULL;
