## 2020-01-04 v1.1.0 - Released
This release contains few bugfixes and also populates few fields that the required to be shown on the UI

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v1.0.0...v1.0.1)

### Bug Fixes
* [MDEXP-94](https://issues.folio.org/browse/MDEXP-94) - Generated binary file is not removed
* [MDEXP-91](https://issues.folio.org/browse/MDEXP-91) - In large data exports - not all records are being exported


### Stories
* [MDEXP-95](https://issues.folio.org/browse/MDEXP-95) - FT index not present
* [MDEXP-80](https://issues.folio.org/browse/MDEXP-80) - Provide Number of Records
* [MDEXP-81](https://issues.folio.org/browse/MDEXP-81) - Provide First name and Last name of the user who triggered the export job
* [MDEXP-79](https://issues.folio.org/browse/MDEXP-79) - Provide Human Readable JobId
* [MDEXP-76](https://issues.folio.org/browse/MDEXP-76) - NPE if none of the records are present in SRS
* [MDEXP-74](https://issues.folio.org/browse/MDEXP-74) - Add file extenstion valiation to /data-export/fileDefinitions and /data-export/fileDefinitions/{id}/upload
* [MDEXP-71](https://issues.folio.org/browse/MDEXP-71) - Tests covering end-end flow


## 2020-13-03 v1.0.0 - Released

Initial release, which exports MARC bibliographic records to AWS S3, for the instance UUIDs which have underlying records in SRS.

* [MDEXP-69] - Update a jobExecution entry in DB
* [MDEXP-68] - Create a S3 bucket for a tenant
* [MDEXP-67] - Export created file to S3
* [MDEXP-66] - Create API GET /data-export/jobExecutions/{jobId}/download/{fileId}
* [MDEXP-65] - Create APIs GET /data-export/jobExecutions to fetch all jobs
* [MDEXP-62] - Create project for API tests
* [MDEXP-61] - Create JobExecution entity
* [MDEXP-60] - POC: Saving files to S3
* [MDEXP-59] - Create PoC for the MappingProcessor
* [MDEXP-58] - Create InputDataManager
* [MDEXP-56] - File Export Storage Implementation
* [MDEXP-50] - Request to include mod-data-export to be deployed on reference environments
* [MDEXP-46] - SPIKE: Investigate saving generated export files
* [MDEXP-30] - Java technology stack
* [MDEXP-28] - Design Diagrams
* [MDEXP-27] - Requirements analysis
* [MDEXP-21] - SPIKE: evaluate the marc4j API for writer capabilities
* [MDEXP-20] - Project Setup
* [MDEXP-8] - Export instance records in MARC based on provided UUIDs
* [MDEXP-6] - Create Inventory Instance to MARC Bib record mapping rules
* [MDEXP-2] - Save binary MARC file in the designated location
* [MDEXP-1] - SPIKE: Investigate the way to read large csv files
