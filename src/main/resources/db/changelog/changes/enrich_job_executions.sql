ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS total INT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS exported INT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS failed INT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS job_profile_name TEXT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS started_date TIMESTAMP;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS run_by_first_name TEXT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS run_by_last_name TEXT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS hrid INT;

do
$$
  declare
    rec record;
  begin
    for rec in
      select cast(jsonb ->> 'id' as uuid) as id,
             cast(jsonb -> 'progress' ->> 'total' as int) as total,
             cast(jsonb -> 'progress' ->> 'exported' as int) as exported,
             cast(jsonb -> 'progress' ->> 'failed' as int) as failed,
             jsonb ->> 'jobProfileName' as jobProfileName,
             to_timestamp(cast(jsonb ->> 'startedDate' AS BIGINT) / 1000) as startedDate,
             jsonb -> 'runBy' ->> 'firstName' as firstName,
             jsonb -> 'runBy' ->> 'lastName' as lastName,
             cast(jsonb ->> 'hrid' as int) as hrid
      from job_executions
      loop
        update job_executions
          set total = rec.total,
              exported = rec.exported,
              failed = rec.failed,
              job_profile_name = rec.jobProfileName,
              started_date = rec.startedDate,
              run_by_first_name = rec.firstName,
              run_by_last_name = rec.lastName,
              hrid = rec.hrid
          where id = rec.id;
      end loop;
  end;
$$
