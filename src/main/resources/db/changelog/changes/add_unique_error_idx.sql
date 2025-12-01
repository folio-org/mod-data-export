CREATE UNIQUE INDEX IF NOT EXISTS error_logs_unique_idx ON error_logs (
  (jsonb->>'jobExecutionId'),
  (jsonb->>'errorMessageCode'),
  (jsonb->'errorMessageValues')
);
