INSERT INTO
  ${myuniversity}_${mymodule}.mapping_profiles (id, jsonb)
VALUES
  (
    '25d81cbe-9686-11ea-bb37-0242ac130002',
    '{
       "id": "25d81cbe-9686-11ea-bb37-0242ac130002",
       "name": "Default instance mapping profile",
       "description": "Default mapping profile for the inventory instance record",
       "recordTypes": [
         "INSTANCE"
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
