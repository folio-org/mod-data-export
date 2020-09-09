UPDATE diku_mod_data_export.job_executions
SET jsonb = jsonb_set(jsonb, '{lastUpdatedDate}', to_jsonb(now()), true)
WHERE jsonb -> 'lastUpdatedDate' IS NULL;
