# mod-data-export

Copyright (C) 2018-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

* [Introduction](#introduction)
* [Compiling](#compiling)
* [Docker](#docker)
* [System requirements](#system-requirements)

## Introduction

FOLIO data export module.

#### Important notes

## LIMITATIONS OF THE MODULE
For current releases, S3-compatible file storage (using MinIO client) is supported
to store the exported MARC files.

## OTHER
In a multi-tenant cluster, each tenant data is stored in it's own folder(prefix) under one bucket. For example, if the tenants are tenant001, tenant002, and bucket name "exportMarc" below is the pattern used stored in the bucket
/{tenantId}/{jobExecutionId}/{fileName}.mrc
- exportMarc/tenant001/448ae575-daec-49c1-8041-d64c8ed8e5b1/testFile-20200116100503.mrc
- exportMarc/tenant002/295e28b4-aea2-4458-9073-385a31e1da05/uc-20200116100503.mrc


Note: There are plans to have hosting agnostic implementation available in 2020 Q2.

## Compiling

```
   mvn install
```

See that it says "BUILD SUCCESS" near the end.

## Docker

Build the docker container with:

```
   docker build -t mod-data-export .
```

Test that it runs with:

```
   docker run -d -t -i -p 8081:8081 mod-data-export
```

## Installing the module

Follow the guide of
[Deploying Modules](https://github.com/folio-org/okapi/blob/master/doc/guide.md#example-1-deploying-and-using-a-simple-module)
sections of the Okapi Guide and Reference, which describe the process in detail.

## Deploying the module

Next we need to deploy the module. There is a deployment descriptor in
`target/DeploymentDescriptor.json`. It tells Okapi to start the module on 'localhost'.

Deploy it via Okapi discovery:

```
curl -w '\n' -D - -s \
  -X POST \
  -H "Content-type: application/json" \
  -d @target/DeploymentDescriptor.json  \
  http://localhost:9130/_/discovery/modules
```

Then we need to enable the module for the tenant:

```
curl -w '\n' -X POST -D -   \
    -H "Content-type: application/json"   \
    -d @target/TenantModuleDescriptor.json \
    http://localhost:9130/_/proxy/tenants/<tenant_name>/modules
```


This module requires default mapping and job profiles for the work. To load them, the `loadRefernce=true` tenant initialization parameter should be passed when installing the module :

```
curl -w '\n' -X POST -d '[ { "id": "mod-data-export-<module_version>", "action": "enable" } ]' http://localhost:9130/_/proxy/tenants/<tenant_name>/install?tenantParameters=loadReference%3Dtrue
```

## Storage configuration
MinIO remote storage or Amazon S3 can be used as storage for generated files MARC files.
The storage is selected by specifying the url of S3-compatible storage by using ENV variable `AWS_URL`.
`AWS_SDK` is used to specify client to communicate with storage.
It requires `true` in case if S3 usage or `false` in case if MinIO usage. By default it equals to `false`.
In addition,the following ENV variables can be specified: `AWS_REGION`, `AWS_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`.

## Issue tracker

See project [MDEXP](https://issues.folio.org/browse/MDEXP)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

## Additional information

### Configure your own mapping rules

To configure your own rules, you need to add it to the [mod-configuration module](https://github.com/folio-org/mod-configuration).
"Code" value to use in the request - "RULES_OVERRIDE".
"Value" field represents the rules, create your rules in the proper format(the example of default data-export rules - https://github.com/folio-org/mod-data-export/blob/master/src/main/resources/rules/rulesDefault.json).
Convert the rules to the String format (you can use online [converter](https://tools.knowledgewalls.com/jsontostring)). Put the result string to the "value" field in the request body.
The description of how to create a configuration in mod-configuration module - https://github.com/folio-org/mod-configuration/blob/master/README.md.
`If there are rules in mod-configuration, and they are enabled, they always will be used for the mapping process for a given tenant.`

## System requirements

Testing was done with a mod-data-export docker image memory limit = 512 MiB.

### Updated system requirements
memory: 844

memoryReservation: 768

cpu: 1024

MetaspaceSize=88m

MaxMetaspaceSize=88m

Xmx=540m

#### Default mapping profile

For default mapping profile, the max usage of memory is 270 MiB during the process of export 1 million records.
The average memory usage is ~200 MiB.

#### Custom mapping profile with holdings and items
For Custom mapping profile, the max usage of memory is 250 MiB during the process of export 1 million records.
The average memory usage is ~190 MiB.

#### MINIO storage memory settings for /data-export/export-all
Depending on the total amount of records to be exported, the size of minio storage required for the successful export can be various.
For example, 1 output mrc file with 100k records occupies 115Mb of minio storage and for 8180068 instances
it requires 82 files, so the total size of files will be 115 * 82 = 9.43Gb for only 1 export execution.
These results should be taken into account when setting up configuration for minio storage, especially for
larger data sets and parallel export.



The [raml-module-builder](https://github.com/folio-org/raml-module-builder) framework.

Other [modules](https://dev.folio.org/source-code/#server-side).

Other FOLIO Developer documentation is at [dev.folio.org](https://dev.folio.org/)

### Environment variables
This module uses S3 storage for files. AWS S3 and Minio Server are supported for files storage.
It is also necessary to specify variable S3_IS_AWS to determine if AWS S3 is used as files storage. By default,
this variable is `false` and means that MinIO server is used as storage.
This value should be `true` if AWS S3 is used.

| Name                                  | Default value          | Description                                |
|:--------------------------------------|:-----------------------|:-------------------------------------------|
| S3_URL                                | http://127.0.0.1:9000/ | S3 url                                     |
| S3_REGION                             | -                      | S3 region                                  |
| S3_BUCKET                             | -                      | S3 bucket                                  |
| S3_ACCESS_KEY_ID                      | -                      | S3 access key                              |
| S3_SECRET_ACCESS_KEY                  | -                      | S3 secret key                              |
| S3_IS_AWS                             | false                  | Specify if AWS S3 is used as files storage |
| EXPORT_TMP_STORAGE                    | -                      | Volume to store exports files              |


