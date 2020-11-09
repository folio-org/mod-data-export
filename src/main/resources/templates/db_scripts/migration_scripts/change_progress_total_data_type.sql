UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb #- '{progress, total}', '{progress, total}', to_jsonb((jsonb -> 'progress' ->> 'total')::int))
WHERE jsonb -> 'progress' IS NOT NULL;
