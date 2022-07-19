INSERT INTO ${myuniversity}_${mymodule}.mapping_profiles(id, jsonb, creation_date, created_by)
VALUES ('5d636597-a59d-4391-a270-4e79d5ba70e3', '{
  "id": "5d636597-a59d-4391-a270-4e79d5ba70e3",
  "name": "Default authority mapping profile",
  "default" : true,
  "description": "Default authority profile for MARC for authority record",
  "recordTypes": [
    "AUTHORITY"
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
VALUES ('56944b1c-f3f9-475b-bed0-7387c33620ce', '{
  "id": "56944b1c-f3f9-475b-bed0-7387c33620ce",
  "name": "Default authority export job profile",
  "default" : true,
  "destination": "fileSystem",
  "description": "Default authority job profile",
  "userInfo": {
    "firstName": "System",
    "lastName": "Process",
    "userName": "system_process"
  },
  "mappingProfileId": "5d636597-a59d-4391-a270-4e79d5ba70e3",
  "metadata":{
    "createdDate":"2020-07-28T00:00:00Z",
    "createdByUserId":"00000000-0000-0000-0000-000000000000",
    "createdByUsername":"system_process",
    "updatedDate":"2020-07-28T00:00:00Z",
    "updatedByUserId":"00000000-0000-0000-0000-000000000000",
    "updatedByUsername":"system_process"
  }
}', NOW(), '00000000-0000-0000-0000-000000000000', '5d636597-a59d-4391-a270-4e79d5ba70e3');
