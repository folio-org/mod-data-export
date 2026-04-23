INSERT INTO mapping_profiles(id, jsonb, creation_date, created_by,
name, description, record_types, format, updated_date, updated_by_user_id, updated_by_first_name, updated_by_last_name)
VALUES (
  'f8b400da-6a0c-4058-be10-cece93265c32',
  '{
     "id": "f8b400da-6a0c-4058-be10-cece93265c32",
     "name": "Default Linked Data instance mapping profile",
     "default" : true,
     "description": "Default mapping profile for the Linked Data instance resource",
     "recordTypes": [
       "LINKED_DATA"
     ],
     "outputFormat": "LINKED_DATA",
     "userInfo": {
       "firstName": "System",
       "lastName": "Process",
       "userName": "system_process"
     },
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
  'Default Linked Data instance mapping profile',
  'Default mapping profile for the Linked Data instance resource',
  'LINKED_DATA',
  'LINKED_DATA',
  '2025-09-24T00:00:00Z',
  '00000000-0000-0000-0000-000000000000',
  'System',
  'Process'
) ON CONFLICT DO NOTHING;

