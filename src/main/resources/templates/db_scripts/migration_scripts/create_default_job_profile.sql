INSERT INTO
  ${myuniversity}_${mymodule}.job_profiles (id, jsonb)
VALUES
  (
    '6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a',
    '{
       "id": "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a",
       "name": "Default job profile",
       "destination": "fileSystem",
       "description": "Default job profile",
       "userInfo": {
         "firstName": "System",
         "lastName": "Process",
         "userName": "system_process"
       },
       "mappingProfileId": "25d81cbe-9686-11ea-bb37-0242ac130002"
    }'
  ) ON CONFLICT DO NOTHING;
