-- enrich_tables.sql fails when trying to convert '' to UUID
DELETE FROM error_logs WHERE jsonb ->> 'jobExecutionId' = '';
