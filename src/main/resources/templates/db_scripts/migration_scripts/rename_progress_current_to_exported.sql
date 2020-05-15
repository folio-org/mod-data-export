UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb #- '{progress, current}', '{progress, exported}', jsonb -> 'progress' -> 'current');
