UPDATE ${myuniversity}_${mymodule}.job_profiles
SET jsonb = jsonb || '{"name":"Default instances export job profile"}'::jsonb || '{"description":"Default instances export job profile"}'::jsonb
WHERE id = '6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a';
