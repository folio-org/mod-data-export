-- mapping profiles
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS name TEXT;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS record_types TEXT;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS format TEXT;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS updated_by_user_id TEXT;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS updated_by_first_name TEXT;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS updated_by_last_name TEXT;

do
$$
  declare
    rec record;
  begin
    for rec in
      select cast(jsonb ->> 'id' as uuid) as id,
             jsonb ->> 'name' as name,
             jsonb ->> 'description' as description,
             (select string_agg(trim(types::text, '"'), ',')
               from jsonb_array_elements(jsonb->'recordTypes') types) as recordTypes,
             jsonb ->> 'outputFormat' as format,
             cast(jsonb -> 'metadata' ->> 'updatedDate' as timestamp) as updatedDate,
             jsonb -> 'metadata' ->> 'updatedByUserId' as userId,
             jsonb -> 'userInfo' ->> 'firstName' as firstName,
             jsonb -> 'userInfo' ->> 'lastName' as lastName
      from mapping_profiles
      loop
        update mapping_profiles
        set name = rec.name,
            description = rec.description,
            record_types = rec.recordTypes,
            format = rec.format,
            updated_date = rec.updatedDate,
            updated_by_user_id = rec.userId,
            updated_by_first_name = rec.firstName,
            updated_by_last_name = rec.lastName
        where id = rec.id;
      end loop;
  end;
$$;

-- job_profiles
ALTER TABLE job_profiles ADD COLUMN IF NOT EXISTS name TEXT;
ALTER TABLE job_profiles ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE job_profiles ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;
ALTER TABLE job_profiles ADD COLUMN IF NOT EXISTS updated_by_user_id TEXT;
ALTER TABLE job_profiles ADD COLUMN IF NOT EXISTS updated_by_first_name TEXT;
ALTER TABLE job_profiles ADD COLUMN IF NOT EXISTS updated_by_last_name TEXT;

do
$$
  declare
    rec record;
  begin
    for rec in
      select cast(jsonb ->> 'id' as uuid) as id,
             jsonb ->> 'name' as name,
             jsonb ->> 'description' as description,
             cast(jsonb -> 'metadata' ->> 'updatedDate' as timestamp) as updatedDate,
             jsonb -> 'metadata' ->> 'updatedByUserId' as userId,
             jsonb -> 'userInfo' ->> 'firstName' as firstName,
             jsonb -> 'userInfo' ->> 'lastName' as lastName
      from job_profiles
      loop
        update job_profiles
        set name = rec.name,
            description = rec.description,
            updated_date = rec.updatedDate,
            updated_by_user_id = rec.userId,
            updated_by_first_name = rec.firstName,
            updated_by_last_name = rec.lastName
        where id = rec.id;
      end loop;
  end;
$$;

-- job_executions
CREATE TYPE ExecutionStatusType AS ENUM ('NEW', 'IN_PROGRESS', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAIL');
CREATE CAST (character varying as ExecutionStatusType) WITH INOUT AS IMPLICIT;

ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS total INT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS exported INT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS failed INT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS job_profile_id TEXT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS job_profile_name TEXT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS started_date TIMESTAMP;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS completed_date TIMESTAMP;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS run_by_id uuid;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS run_by_first_name TEXT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS run_by_last_name TEXT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS hrid INT;
ALTER TABLE job_executions ADD COLUMN IF NOT EXISTS status ExecutionStatusType;

do
$$
  declare
    rec record;
  begin
    for rec in
      select cast(jsonb ->> 'id' as uuid) as id,
             cast(jsonb -> 'progress' ->> 'total' as int) as total,
             cast(jsonb -> 'progress' ->> 'exported' as int) as exported,
             cast(jsonb -> 'progress' -> 'failed' ->> 'duplicatedSrs' as int) as duplicatedSrs,
             cast(jsonb -> 'progress' -> 'failed' ->> 'otherFailed' as int) as failed,
             jsonb ->> 'jobProfileName' as jobProfileName,
             cast(jsonb ->> 'startedDate' as timestamp) as startedDate,
             cast(jsonb ->> 'completedDate' as timestamp) as completedDate,
             cast(jsonb -> 'runBy' ->> 'userId' as uuid) as userId,
             jsonb -> 'runBy' ->> 'firstName' as firstName,
             jsonb -> 'runBy' ->> 'lastName' as lastName,
             cast(jsonb ->> 'hrId' as int) as hrid,
             cast(jsonb ->> 'status' as ExecutionStatusType) as status
      from job_executions
      loop
        update job_executions
          set jsonb = jsonb_set(jsonb, '{progress}', jsonb_build_object('total', rec.total, 'exported', rec.exported, 'duplicatedSrs', rec.duplicatedSrs, 'failed', rec.failed)),
              total = rec.total,
              exported = rec.exported,
              failed = rec.failed,
              job_profile_name = rec.jobProfileName,
              started_date = rec.startedDate,
              completed_date = rec.completedDate,
              run_by_id = rec.userId,
              run_by_first_name = rec.firstName,
              run_by_last_name = rec.lastName,
              hrid = rec.hrid,
              status = rec.status
          where id = rec.id;
      end loop;
  end;
$$;

-- error_logs
ALTER TABLE error_logs ADD COLUMN IF NOT EXISTS job_execution_id uuid;

do
$$
  declare
    rec record;
  begin
    for rec in
      select cast(jsonb ->> 'id' as uuid) as id,
             cast(jsonb ->> 'jobExecutionId' as uuid) as jobExecutionId
      from error_logs
      loop
        update error_logs
        set job_execution_id = rec.jobExecutionId
        where id = rec.id;
      end loop;
  end;
$$;
