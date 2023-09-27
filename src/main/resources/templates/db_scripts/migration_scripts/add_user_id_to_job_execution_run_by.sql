UPDATE ${myuniversity}_${mymodule}.job_executions job_executions SET jsonb=jsonb_set(job_executions.jsonb, '{runBy, userId}',  concat('"', users.id, '"')::jsonb)
FROM ${myuniversity}_mod_users.users users
WHERE
job_executions.jsonb->'runBy'->'userId' IS null AND
job_executions.jsonb->'runBy'->>'lastName'=users.jsonb->'personal'->>'lastName' AND
job_executions.jsonb->'runBy'->>'firstName'=users.jsonb->'personal'->>'firstName';

