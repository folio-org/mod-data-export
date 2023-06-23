UPDATE ${myuniversity}_${mymodule}.job_executions AS executions
SET jsonb = jsonb_set(jsonb, '{jobProfileName}', (
    SELECT profiles.jsonb->'name' FROM ${myuniversity}_${mymodule}.job_profiles AS profiles
      WHERE profiles.id::text = executions.jsonb->>'jobProfileId'
  ))
WHERE executions.jsonb -> 'jobProfileId' IS NOT NULL AND executions.jsonb -> 'jobProfileName' IS NULL;