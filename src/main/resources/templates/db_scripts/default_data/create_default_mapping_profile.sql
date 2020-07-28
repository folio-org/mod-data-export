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
       }
    }'
  ) ON CONFLICT DO NOTHING;
