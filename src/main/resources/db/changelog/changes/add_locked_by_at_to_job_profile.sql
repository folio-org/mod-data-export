ALTER TABLE job_profiles ADD COLUMN IF NOT EXISTS locked_by UUID;
ALTER TABLE job_profiles ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP;
UPDATE job_profiles SET locked = true, locked_by = '00000000-0000-0000-0000-000000000000',
                        locked_at = NOW(),
                        jsonb = jsonb_set(
                            jsonb_set(
                                jsonb_set(jsonb, '{locked}', 'true', true),
                                '{lockedBy}', '"00000000-0000-0000-0000-000000000000"', true),
                            '{lockedAt}', to_jsonb(NOW()), true)
                    WHERE id IN (
'56944b1c-f3f9-475b-bed0-7387c33620ce',
'2c9be114-6d35-4408-adac-9ead35f51a27',
'5e9835fc-0e51-44c8-8a47-f7b8fce35da7',
'6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a',
'42ca0945-f66c-4bc1-8d1a-7aa8b2e4483a');
