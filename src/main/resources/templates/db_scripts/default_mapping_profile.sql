INSERT INTO ${myuniversity}_${mymodule}.mapping_profiles (id, jsonb) values
('25d81cbe-9686-11ea-bb37-0242ac130002', '{
  "id": "25d81cbe-9686-11ea-bb37-0242ac130002",
  "name": "Default instance mapping profile",
  "description": "Default mapping profile for the inventory instance record",
  "recordTypes": [ "INSTANCE" ],
  "outputFormat": "MARC",
  "userInfo" : {
        "firstName" : "DIKU",
        "lastName" : "ADMINISTRATOR",
        "userName" : "diku_admin"
  },
  "metadata": {
      "createdDate": "2020-05-15T09:39:13.659+0000",
      "createdByUserId": "",
      "updatedDate": "2020-05-15T09:39:13.659+0000",
      "updatedByUserId": ""
    }
  }'
) ON CONFLICT DO NOTHING;
