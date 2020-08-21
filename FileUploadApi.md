# *File Upload API*

## *Introduction*

The *mod-data-export* is responsible for exporting FOLIO records into a specified output format into the given destination. 
The list of the FOLIO records can be defined in 2 ways: CSV file with listed identifiers or CQL query. 

This documentation describes the REST API to upload CSV files to the mod-data-export for further processing.

### *FileDefinition*

File Definition [schema](https://github.com/folio-org/mod-data-export/blob/master/ramls/schemas/fileDefinition.json) 
describes an upload process of a particular file. The creation of the FileDefinition entity is the first step of the 
file upload process.

| Field | Description |
| ------ | ------ |
| id | UUID for a FileDefinition entity |
| fileName | Name of the file with its extension. This  field is required |
| jobExecutionId | UUID of the file's JobExecution process. Specific for data-export application |
| sourcePath | The path to the file location in the storage. Read only |
| status | Show the current state of the upload file.  |
| size | Size of the file in Kbyte. |
| metadata | An object that stores information about creating and updating fileDefinition |

#### MetaData

Located in FileDefinition entity and stores information about creating and updating fileDefinition

| Field | Description |
| ------ | ------ |
| createdDate | Date and time when the file was created |
| createdByUserId | The user id who created the file |
| createdByUsername | The username who created the file |
| updatedDate | Date and time when the file was updated last time |
| updatedByUserId | The user id who updated the file |
| updatedByUsername | The username who updated the file |

#### Status

This ENUM located in FileDefinition entity and stores information about its current status.

| Possible values | Description |
| ------ | ------ |
| NEW |The fileDefinition is created but uploaded process did not start yet |
| IN_PROGRESS | The file upload process is currently in progress |
| COMPLETED | The file upload process completed |
| ERROR | The file upload process failed |

### *REST API*

For all necessary file upload lifecycle operations mod-data-export provides following endpoints: 

| Method | URL | ContentType |Description |
| ------ |------ | ------ |------ |
| **POST** | /data-export/file-definitions | application/json | Endpoint to create file definition to use it for the file uploading |
| **GET** |/data-export/file-definitions/**{fileDefinitionId}** | application/json | Endpoint to get file definition by id |
| **POST** | /data-export/file-definitions/**{fileDefinitionId}**/upload | application/octet-stream | Endpoint to upload file |

#### File Upload Workflow
                         
Before working with API make sure you have an HTTP token that is required for sending requests. If you have already 
logged in the system using UI, just copy token from `Apps/Settings/Developer/Set token/Authentication token` field.
Also, you can log in to the system using CLI tools, response returns `x-okapi-token` header with HTTP token.
```
Login request:
curl -w '\n' -X POST \
  --header "Content-Type: application/json" \
  --header "x-okapi-tenant: {tenant}" \
  --data @credentials.json \
  https://folio-snapshot-load-okapi.aws.indexdata.com/bl-users/login

  credentials.json: 
  {
    "username": "{username}",
    "password": "{password}"
  }
```
Send POST request with file name for creating new FileDefinition.

```
curl -w '\n' -X POST -D -   \
   -H "Content-type: application/json"   \
   -H "x-okapi-tenant: diku"  \
   -H "x-okapi-token: {token} \
   -d @fileDefinitionRequest.json \
   http://localhost:9130/data-export/file-definitions
```

##### fileDefinitionRequest.json

```
{  
         "fileName":"inventoryUUIDs.csv"
}
```

##### Response with FileDefinition

After creating the FileDefinition, the status of the entity is "NEW"

```
{
  "id": {id},
  "fileName": "inventoryUUIDs.csv",
  "status": "NEW",
  "metadata": {
    "createdDate": "2020-02-17T11:05:29.905+0000",
    "createdByUserId": "8632f620-d4d1-563b-8719-e36c1364385e",
    "updatedDate": "2020-02-17T11:05:29.905+0000",
    "updatedByUserId": "8632f620-d4d1-563b-8719-e36c1364385e"
  }
}

```

#### Upload file body to the backend
After FileDefinition successfully created, backend ready to file uploading. 
Content-type: application/octet-stream. Set the fileDefinition id to the url.

```
curl -w '\n' -X POST -D -   \
   -H "Content-type: application/octet-stream"   \
   -H "x-okapi-tenant: diku"  \
   -H "x-okapi-token: {token} \
   -d @inventoryUUIDs.csv \
   http://localhost:9130/data-export/file-definitions/{id}/upload
```

##### Response with changed UploadDefinition after upload completed

If the file is loaded, FileDefinition change status to "COMPLETED" and added new field "sourcePath" with the path to the file.

```
{
  "id": {id},
  "fileName": "inventoryUUIDs.csv",
  "sourcePath": "./storage/files/{id}/inventoryUUIDs.csv",
  "status": "COMPLETED",
  "metadata": {
    "createdDate": "2020-02-17T11:05:29.905+0000",
    "createdByUserId": "8632f620-d4d1-563b-8719-e36c1364385e",
    "updatedDate": "2020-02-17T11:05:29.905+0000",
    "updatedByUserId": "8632f620-d4d1-563b-8719-e36c1364385e"
  }
}
```

#### GET FileDefinition

##### Request for getting file definition by id

```
curl -X GET -D - -w '\n' \
  -H "x-okapi-tenant: diku"  \
  -H "x-okapi-token: {token} \
  http://localhost:9130/data-export/file-definitions/{id}     
```

##### Response for getting file definition by id

```
{
  "id": {id},
  "fileName": "inventoryUUIDs.csv",
  "sourcePath": "./storage/files/{id}/inventoryUUIDs.csv",
  "status": "COMPLETED",
  "metadata": {
    "createdDate": "2020-02-17T11:05:29.905+0000",
    "createdByUserId": "8632f620-d4d1-563b-8719-e36c1364385e",
    "updatedDate": "2020-02-17T11:05:29.905+0000",
    "updatedByUserId": "8632f620-d4d1-563b-8719-e36c1364385e"
  }
}
```

### *File cleaning process*

The **mod-data-export** module has its own automatic file cleaning mechanism. Once an hour the periodic job removes files 
which were created 1 hour ago.
