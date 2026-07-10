-- Remove duplicates that would violate error_logs_unique_idx before the index is created.
DELETE FROM error_logs e
USING error_logs d
WHERE e.id > d.id
  AND e.jsonb ->> 'jobExecutionId' = d.jsonb ->> 'jobExecutionId'
  AND e.jsonb ->> 'errorMessageCode' = d.jsonb ->> 'errorMessageCode'
  AND md5((e.jsonb -> 'errorMessageValues')::text) = md5((d.jsonb -> 'errorMessageValues')::text);

