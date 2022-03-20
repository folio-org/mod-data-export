INSERT INTO ${myuniversity}_${mymodule}.mapping_profiles(
	id, jsonb, creation_date, created_by)
	VALUES ('1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13', '{
  "id": "1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13",
  "name": "Default holdings mapping profile",
  "default" : true,
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
}', NOW(), '00000000-0000-0000-0000-000000000000');

INSERT INTO ${myuniversity}_${mymodule}.job_profiles(
	id, jsonb, creation_date, created_by, mappingprofileid)
	VALUES ('5e9835fc-0e51-44c8-8a47-f7b8fce35da7', '{
  "id": "5e9835fc-0e51-44c8-8a47-f7b8fce35da7",
  "name": "Default holdings export job profile",
  "default" : true,
  "destination": "fileSystem",
  "description": "Default holdings job profile",
  "userInfo": {
    "firstName": "System",
    "lastName": "Process",
    "userName": "system_process"
  },
  "mappingProfileId": "1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13",
  "metadata":{
    "createdDate":"2020-07-28T00:00:00Z",
    "createdByUserId":"00000000-0000-0000-0000-000000000000",
    "createdByUsername":"system_process",
    "updatedDate":"2020-07-28T00:00:00Z",
    "updatedByUserId":"00000000-0000-0000-0000-000000000000",
    "updatedByUsername":"system_process"
  }
}', NOW(), '00000000-0000-0000-0000-000000000000', '1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13');
