UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb, '{lastUpdatedDate}', to_jsonb(now()), true)
WHERE jsonb -> 'lastUpdatedDate' IS NULL and
      jsonb -> 'completedDate' IS NULL;


UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb, '{lastUpdatedDate}', jsonb->'completedDate', true)
WHERE jsonb -> 'lastUpdatedDate' IS NULL and
      jsonb -> 'completedDate' IS NOT NULL;
