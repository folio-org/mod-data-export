UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb #- '{progress, total}', '{progress, total}', to_jsonb((jsonb -> 'progress' ->> 'total')::int))
WHERE jsonb -> 'progress' IS NOT NULL AND jsonb -> 'progress' <> '{}';

UPDATE diku_mod_data_export.job_executions
SET jsonb = jsonb_set(jsonb, '{progress}', '{"total": 0, "failed": 0, "exported": 0}'::jsonb)
WHERE jsonb -> 'progress' IS NULL OR jsonb -> 'progress' = '{}';
