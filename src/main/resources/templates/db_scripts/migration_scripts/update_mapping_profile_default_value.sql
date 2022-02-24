UPDATE ${myuniversity}_${mymodule}.mapping_profiles
SET jsonb = jsonb_set(jsonb, '{default}', 'true')
WHERE id = '1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13' OR id = '25d81cbe-9686-11ea-bb37-0242ac130002';

UPDATE ${myuniversity}_${mymodule}.mapping_profiles
SET jsonb = jsonb_set(jsonb, '{default}', 'false')
WHERE id != '1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13' OR id != '25d81cbe-9686-11ea-bb37-0242ac130002';
