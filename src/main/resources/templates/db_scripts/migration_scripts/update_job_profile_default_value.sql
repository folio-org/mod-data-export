UPDATE ${myuniversity}_${mymodule}.job_profiles
SET jsonb = jsonb_set(jsonb, '{default}', 'true')
WHERE id = '5e9835fc-0e51-44c8-8a47-f7b8fce35da7' OR id = '6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a';

UPDATE ${myuniversity}_${mymodule}.job_profiles
SET jsonb = jsonb_set(jsonb, '{default}', 'false')
WHERE id != '5e9835fc-0e51-44c8-8a47-f7b8fce35da7' AND id != '6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a';
