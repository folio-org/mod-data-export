# mod-data-export

Copyright (C) 2018-2019 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

* [Introduction](#introduction)
* [Compiling](#compiling)
* [Docker](#docker)

## Introduction

FOLIO data export module.

#### Important notes

## LIMITATIONS OF THE MODULE
For Q1 release, the only supported file storage to store the exported MARC files is S3. 
This means that it is expected that AWS credentials are properly configured in a hosting environment as per:https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html.
The credential chain looks for the credentials in multiple places and the order of precedence in case it is specified in multiple places is given here: https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html

## CONFIGURATIONS NEEDED
The module also expects the bucket name to be passed in the JVM as the system parameter with name `bucket.name`

example: java mod-data-export-fat.jar -Dbucket.name=diku_export


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

## Issue tracker

See project [MDEXP](https://issues.folio.org/browse/MDEXP)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

## Additional information

### Configure your own mapping rules

To configure your own rules, you need to add it to the [mod-configuration module](https://github.com/folio-org/mod-configuration).
"Code" value to use in the request - "DATA_EXPORT".
"Value" field represents the rules, create your rules in the proper format(the example of default data-export rules - https://github.com/folio-org/mod-data-export/blob/master/src/main/resources/rules/rulesDefault.json).
Convert the rules to the String format (you can use online [converter](https://tools.knowledgewalls.com/jsontostring)). Put the result string to the "value" field in the request body. 
The description of how to create a configuration in mod-configuration module - https://github.com/folio-org/mod-configuration/blob/master/README.md.
`If there are rules in mod-configuration, they always will be used for the mapping process for a given tenant.`


The [raml-module-builder](https://github.com/folio-org/raml-module-builder) framework.

Other [modules](https://dev.folio.org/source-code/#server-side).

Other FOLIO Developer documentation is at [dev.folio.org](https://dev.folio.org/)
