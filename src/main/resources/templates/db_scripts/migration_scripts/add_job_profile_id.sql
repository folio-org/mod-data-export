UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb , '{jobProfileId}', '"6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a"')
WHERE jsonb -> 'jobProfileId' IS NULL;
