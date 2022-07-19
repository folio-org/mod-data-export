INSERT INTO ${myuniversity}_${mymodule}.mapping_profiles(id, jsonb, creation_date, created_by)
VALUES ('25d81cbe-9686-11ea-bb37-0242ac130002', '{
  "id": "25d81cbe-9686-11ea-bb37-0242ac130002",
  "name": "Default instance mapping profile",
  "default" : true,
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
}', NOW(), '00000000-0000-0000-0000-000000000000');

INSERT INTO ${myuniversity}_${mymodule}.job_profiles(id, jsonb, creation_date, created_by, mappingprofileid)
VALUES ('6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a', '{
  "id": "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a",
  "name": "Default instances export job profile",
  "default" : true,
  "destination": "fileSystem",
  "description": "Default instances export job profile",
  "userInfo": {
    "firstName": "System",
    "lastName": "Process",
    "userName": "system_process"
  },
  "mappingProfileId": "25d81cbe-9686-11ea-bb37-0242ac130002",
  "metadata":{
    "createdDate":"2020-07-28T00:00:00Z",
    "createdByUserId":"00000000-0000-0000-0000-000000000000",
    "createdByUsername":"system_process",
    "updatedDate":"2020-07-28T00:00:00Z",
    "updatedByUserId":"00000000-0000-0000-0000-000000000000",
    "updatedByUsername":"system_process"
  }
}', NOW(), '00000000-0000-0000-0000-000000000000', '25d81cbe-9686-11ea-bb37-0242ac130002');
