INSERT INTO job_profiles(id, jsonb, creation_date, created_by, mappingprofileid,
name, description, updated_date, updated_by_user_id, updated_by_first_name, updated_by_last_name)
SELECT
  '42ca0945-f66c-4bc1-8d1a-7aa8b2e4483a',
  '{
     "id": "42ca0945-f66c-4bc1-8d1a-7aa8b2e4483a",
     "name": "Default linked data export job profile",
     "default" : true,
     "destination": "fileSystem",
     "description": "Default linked data job profile",
     "userInfo": {
       "firstName": "System",
       "lastName": "Process",
       "userName": "system_process"
     },
     "mappingProfileId": "f8b400da-6a0c-4058-be10-cece93265c32",
     "metadata":{
       "createdDate":"2025-09-24T00:00:00Z",
       "createdByUserId":"00000000-0000-0000-0000-000000000000",
       "createdByUsername":"system_process",
       "updatedDate":"2025-09-24T00:00:00Z",
       "updatedByUserId":"00000000-0000-0000-0000-000000000000",
       "updatedByUsername":"system_process"
     }
   }',
  '2025-09-24T00:00:00Z',
  '00000000-0000-0000-0000-000000000000',
  'f8b400da-6a0c-4058-be10-cece93265c32',
  'Default linked data export job profile',
  'Default linked data job profile',
  '2025-09-24T00:00:00Z',
  '00000000-0000-0000-0000-000000000000',
  'System',
  'Process'
WHERE EXISTS (SELECT 1 FROM mapping_profiles WHERE id='f8b400da-6a0c-4058-be10-cece93265c32') ON CONFLICT DO NOTHING;

