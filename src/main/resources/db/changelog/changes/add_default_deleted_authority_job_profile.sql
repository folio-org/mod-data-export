INSERT INTO job_profiles(id, jsonb, creation_date, created_by, mappingprofileid,
name, description, updated_date, updated_by_user_id, updated_by_first_name, updated_by_last_name)
SELECT
  '2c9be114-6d35-4408-adac-9ead35f51a27',
  '{
     "id": "2c9be114-6d35-4408-adac-9ead35f51a27",
     "name": "Deleted authority export job profile",
     "default" : true,
     "destination": "fileSystem",
     "description": "Deleted authority export job profile",
     "userInfo": {
       "firstName": "System",
       "lastName": "Process",
       "userName": "system_process"
     },
     "mappingProfileId": "5d636597-a59d-4391-a270-4e79d5ba70e3",
     "metadata":{
       "createdDate":"2024-03-14T00:00:00Z",
       "createdByUserId":"00000000-0000-0000-0000-000000000000",
       "createdByUsername":"system_process",
       "updatedDate":"2024-03-14T00:00:00Z",
       "updatedByUserId":"00000000-0000-0000-0000-000000000000",
       "updatedByUsername":"system_process"
     }
   }',
  '2024-03-14T00:00:00Z',
  '00000000-0000-0000-0000-000000000000',
  '5d636597-a59d-4391-a270-4e79d5ba70e3',
  'Deleted authority export job profile',
  'Deleted authority export job profile',
  '2024-03-14T00:00:00Z',
  '00000000-0000-0000-0000-000000000000',
  'System',
  'Process'
WHERE EXISTS (SELECT 1 FROM mapping_profiles WHERE id='5d636597-a59d-4391-a270-4e79d5ba70e3') ON CONFLICT DO NOTHING;

