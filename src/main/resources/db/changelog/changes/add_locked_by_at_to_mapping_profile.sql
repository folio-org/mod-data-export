ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS locked_by UUID;
ALTER TABLE mapping_profiles ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP;

DATE mapping_profiles SET locked = true, locked_by = '00000000-0000-0000-0000-000000000000',
                        locked_at = NOW(),
                        jsonb = jsonb_set(
                            jsonb_set(
                                jsonb_set(jsonb, '{locked}', 'true', true),
                                '{lockedBy}', '"00000000-0000-0000-0000-000000000000"', true),
                            '{lockedAt}', to_jsonb(NOW()), true)
                    WHERE id IN (
'5d636597-a59d-4391-a270-4e79d5ba70e3',
'1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13',
'25d81cbe-9686-11ea-bb37-0242ac130002',
'f8b400da-6a0c-4058-be10-cece93265c32');

