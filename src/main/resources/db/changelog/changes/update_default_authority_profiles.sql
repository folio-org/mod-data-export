UPDATE ${myuniversity}_mod_data_export.job_profiles
	SET jsonb = jsonb || '{"default": true}'
	WHERE id = '56944b1c-f3f9-475b-bed0-7387c33620ce' OR id = '2c9be114-6d35-4408-adac-9ead35f51a27';

UPDATE ${myuniversity}_mod_data_export.mapping_profiles
	SET jsonb = jsonb || '{"default": true}'
	WHERE id = '5d636597-a59d-4391-a270-4e79d5ba70e3';
