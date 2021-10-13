INSERT INTO
  ${myuniversity}_${mymodule}.mapping_profiles (id, jsonb)
VALUES
  (
  '1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13',
  '{
     "id": "1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13",
     "name": "Default holdings mapping profile",
     "description": "Default mapping profile for MARC for holdings record",
     "recordTypes": [
       "HOLDINGS"
     ],
     "outputFormat": "MARC",
     "userInfo": {
       "firstName": "System",
       "lastName": "Process",
       "userName": "system_process"
     },
     "metadata":{
      "createdDate":"2020-07-28T00:00:00Z",
      "createdByUserId":"00000000-0000-0000-0000-000000000000",
      "createdByUsername":"system_process",
      "updatedDate":"2020-07-28T00:00:00Z",
      "updatedByUserId":"00000000-0000-0000-0000-000000000000",
      "updatedByUsername":"system_process"
    }
  }'
  ) ON CONFLICT(id) DO UPDATE SET jsonb = EXCLUDED.jsonb;
