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

For Q1 release, we are using S3 to store the exported MARC files. This means that it is expected that AWS credentials are properly configured in a hosting environment as per:
https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html. The module also expects the bucket name to be passed in the system variable `bucket.name`

There are plans to have hosting agnostic implementation available in 2020 Q2.

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

The [raml-module-builder](https://github.com/folio-org/raml-module-builder) framework.

Other [modules](https://dev.folio.org/source-code/#server-side).

Other FOLIO Developer documentation is at [dev.folio.org](https://dev.folio.org/)
