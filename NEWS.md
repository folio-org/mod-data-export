## v4.6.0 Unreleased

## 07/15/2022 v4.5.1 Released
This release includes migration scripts fix for mappings and job profiles

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.5.0...v4.5.1)

### Bug Fixes

* [MDEXP-543](https://issues.folio.org/browse/MDEXP-543) Migration fails - mapping and job profiles

## 07/08/2022 v4.5.0 Released
This release includes RMB v34 upgrade, export MARC authority records, default profiles implementation

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.4.2...v4.5.0)

### Technical tasks

* [MDEXP-533](https://issues.folio.org/browse/MDEXP-533) RMB v34 upgrade - Morning Glory 2022 R2 module release
* [MDEXP-511](https://issues.folio.org/browse/MDEXP-511) Migrate to use new api-lint and api-doc CI tools

### Stories

* [MDEXP-513](https://issues.folio.org/browse/MDEXP-513) Support quickExport for Authorities IDs
* [MDEXP-508](https://issues.folio.org/browse/MDEXP-508) Spike: Export MARC authority records
* [MDEXP-487](https://issues.folio.org/browse/MDEXP-487) Spike: Limit file upload size
* [MDEXP-213](https://issues.folio.org/browse/MDEXP-213) Implement a way to identify default profiles (instances and holdings)

### Bug Fixes

* [MDEXP-519](https://issues.folio.org/browse/MDEXP-519) Holdings export: subfields for 866, 867 and 868 are entered separately

## 04/06/2022 v4.4.2 Released
This release includes build failure fix

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.4.1...v4.4.2)

### Bug Fixes

* [MDEXP-509](https://issues.folio.org/browse/MDEXP-509) build-platform-complete-snapshot build failures 3/21

## 04/05/2022 v4.4.1 Released
This release includes improvements of data export flow with custom profiles

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.4.0...v4.4.1)

### Stories

* [MDEXP-516](https://issues.folio.org/browse/MDEXP-516) Preserve LDR 05, 06, 07 values on the export with custom mapping profiles

## 03/21/2022 v4.4.0 Released
This release includes implemented export flow for holdings MFHD records and bug fixes

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.3.0...v4.4.0)

### Bug Fixes

* [MDEXP-477](https://issues.folio.org/browse/MDEXP-477) Triggering the export with cql statement takes longer than expected
* [MDEXP-505](https://issues.folio.org/browse/MDEXP-505) Undefined permission 'data-export.mapping-profiles.collection.get , ...
* [MDEXP-507](https://issues.folio.org/browse/MDEXP-507) Invalid migration scripts for job profiles

## 03/03/2022 v4.3.0 Released
This release includes implemented export flow for holdings MFHD records and bug fixes

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.2.4...v4.3.0)

### Stories

* [MDEXP-99](https://issues.folio.org/browse/MDEXP-99) Generate MARC bib record additional fields if present -- array of strings to repeatable MARC field
* [MDEXP-104](https://issues.folio.org/browse/MDEXP-104) Generate MARC bib record - Alternate titles - conditionally replacing keys with values
* [MDEXP-124](https://issues.folio.org/browse/MDEXP-124) Generate MARC bib record - 500$a/notes
* [MDEXP-303](https://issues.folio.org/browse/MDEXP-303) Generate MFHD records on the fly
* [MDEXP-432](https://issues.folio.org/browse/MDEXP-432) Generate MARC bib record - Identifiers part IV - appending subfields
* [MDEXP-433](https://issues.folio.org/browse/MDEXP-433) Generate MARC bib record - Identifiers part V
* [MDEXP-434](https://issues.folio.org/browse/MDEXP-434) Generate MARC bib record - Identifiers part VI
* [MDEXP-437](https://issues.folio.org/browse/MDEXP-437) Create default mapping profile for holdings
* [MDEXP-438](https://issues.folio.org/browse/MDEXP-438) Create default job profile for holdings
* [MDEXP-439](https://issues.folio.org/browse/MDEXP-439) Rename Default job profile to Default instance export job profile

### Bug Fixes

* [MDEXP-499](https://issues.folio.org/browse/MDEXP-499) Existing holdings UUIDs are reported as invalid while exporting holdings record
* [MDEXP-504](https://issues.folio.org/browse/MDEXP-504) Exported holdings MARC records are combined with the records generated on the fly

## 10/15/2021 v4.2.1 Released
This release includes module configuration improvement

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.2.0...v4.2.1)

### Stories
* [MDEXP-455](https://issues.folio.org/browse/MDEXP-455) Improve MinIO configuration process

## 10/08/2021 v4.2.0 Released
This release includes bug fixes, code improvements, RMB upgrade, unexpected error handling.

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.1.0...v4.2.0)

### Stories
* [MDEXP-429](https://issues.folio.org/browse/MDEXP-429) Update generate-marc-utils version to 1.2.0-SNAPSHOT
* [MDEXP-422](https://issues.folio.org/browse/MDEXP-422) Gracefully handle MARC records that exceed the size limit
* [MDEXP-398](https://issues.folio.org/browse/MDEXP-398) Improve code coverage on new code
* [MDEXP-395](https://issues.folio.org/browse/MDEXP-395) data-export- Upgrade to RMB 33
* [MDEXP-394](https://issues.folio.org/browse/MDEXP-394) Memory leaks in mod-data-export
* [MDEXP-180](https://issues.folio.org/browse/MDEXP-180) Implement using minio as gateway for AWS

### Bug Fixes
* [MDEXP-440](https://issues.folio.org/browse/MDEXP-440) Reference data is missing when there are more than 200 entities.
* [MDEXP-430](https://issues.folio.org/browse/MDEXP-430) Leader Record status changes from "c" to "n" when exporting holdings and items data
* [MDEXP-426](https://issues.folio.org/browse/MDEXP-426) Escape control characters in quoted literals
* [MDEXP-425](https://issues.folio.org/browse/MDEXP-425) When a highly inefficient query is done, vertx main thread is blocked
* [MDEXP-421](https://issues.folio.org/browse/MDEXP-421) Export job triggered with invalid data stays in status New (and does not fail)
* [MDEXP-409](https://issues.folio.org/browse/MDEXP-409) DaoImpl: Use single id methods for get, delete, update
* [MDEXP-402](https://issues.folio.org/browse/MDEXP-402) Migration Script does not handle all cases
* [MDEXP-400](https://issues.folio.org/browse/MDEXP-400) Failed jobs listed always on top of the queue when completeDate is missing

## 11/06/2021 v4.1.0 Released
This release includes the interface version bumping for SRS and mod-inventory related dependencies.

### Stories
* [MDEXP-403](https://issues.folio.org/browse/MDEXP-403) align dependency versions affected by Inventory's Optimistic Locking
* [MDEXP-407](https://issues.folio.org/browse/MDEXP-407) Update srs interface version 

### Bug Fixes
* [MDEXP-388](https://issues.folio.org/browse/MDEXP-388) 0% coverage reported by Sonarcloud for mod-data-export

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.0.1...v4.1.0)

### Bug Fixes
* [MDEXP-338](https://issues.folio.org/browse/MDEXP-338) - Fix periodically wrong jobExecution state when uploading an empty file

## 04/02/2021 v4.0.1 - Released
This bugfix release includes fix for inconsistent results when holdings and items data incorrectly appended to the srs record
in MARC file.

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v4.0.0...v4.0.1)

### Bug Fixes
* [MDEXP-385](https://issues.folio.org/browse/MDEXP-385) - Holdings and items data incorrectly appended to the srs record

## 03/12/2020 v4.0.0 Released
 Major version release which includes below features :
 * allow a user to append holdings and item data with MARC bib record when the user wants to export the record from SRS
 * improve validation for mapping profile transformations: add general backend validation during saving; allow to pass transformations with empty value; allow characters in indicator fields  
 * improve error logs: reflect in error logs what record exactly leads to the exception during the export and append exactly the field name that causes the error; append a link to the inventory record if the export fails
 * add a quick export feature: provide an endpoint to trigger the export based on a provided list of identifiers or CQL statement and can be used in triggering exports by external scripts
 * add more Instance fields to append during generating MARC bib record on the fly: Invalid ISBN, Linking ISBN, Invalid ISSN, Cancelled GPO Item Number identifiers; other standard identifiers, publisher or distributor number, and canceled system control numbers
 * add specific MARC for Holdings record fields mapping rules to map holdings data during generating MARC bib record on the fly
 * add personal data disclosure form
 * upgrade RMB to version 32.2.0
 
 [Full Changelog](https://github.com/folio-org/mod-data-export/compare/v3.0.4...v4.0.0)
 
### Stories
* [MDEXP-265](https://issues.folio.org/browse/MDEXP-265) - Applying mapping profile - holdings and item transformations provided - entire SRS record
* [MDEXP-315](https://issues.folio.org/browse/MDEXP-315) - Add validation to the transformation elements
* [GMU-1](https://issues.folio.org/browse/GMU-1) - Detect which record exactly leads to the exception during the export to the error logs
* [MDEXP-262](https://issues.folio.org/browse/MDEXP-262) - Create a link to the inventory record if the export fails
* [MDEXP-285](https://issues.folio.org/browse/MDEXP-285) - Provide endpoint for quick inventory instance export
* [MDEXP-182](https://issues.folio.org/browse/MDEXP-182) - Generate MARC bib record - Identifiers part II - conditionally replacing keys with values
* [MDEXP-344](https://issues.folio.org/browse/MDEXP-344) - Allow to pass transformations with empty transformation
* [MDEXP-348](https://issues.folio.org/browse/MDEXP-348) - Allow characters in indicator fields
* [MDEXP-349](https://issues.folio.org/browse/MDEXP-349) - Enhancements to /data-export/quick-export endpoint
* [MDEXP-346](https://issues.folio.org/browse/MDEXP-346) - The field name is missing if the reference name has changed
* [MDEXP-12](https://issues.folio.org/browse/MDEXP-12) - Create Inventory Holdings to MFHD record mapping rules  
* [MDEXP-353](https://issues.folio.org/browse/MDEXP-353) - Upgrade to RMB 32
* [MDEXP-191](https://issues.folio.org/browse/MDEXP-191) - Generate MARC bib record - Identifiers part III - appending subfields
* [MDEXP-358](https://issues.folio.org/browse/MDEXP-358) - Add personal data disclosure form
* [GMU-2](https://issues.folio.org/browse/GMU-2) - Remove RMB and vertx dependencies
* [MDEXP-372](https://issues.folio.org/browse/MDEXP-372) - Update dependencies in data-export before Iris release

### Bug Fixes
* [MDEXP-318](https://issues.folio.org/browse/MDEXP-318) - Missing error code for some error messages
* [MDEXP-351](https://issues.folio.org/browse/MDEXP-351) - Missing directory and data in leader when exporting holdings only records with custom mapping profile
* [MDEXP-343](https://issues.folio.org/browse/MDEXP-343) - Fix unstable StorageCleanupServiceImplTest
* [MDEXP-347](https://issues.folio.org/browse/MDEXP-347) - Holdings and items data is not appended to SRS entire record in custom mapping profile
* [MDEXP-345](https://issues.folio.org/browse/MDEXP-345) - Subfield $3 not always present when multiple holdings and items are associated with the instance
* [MDEXP-360](https://issues.folio.org/browse/MDEXP-360) - Most of data-export APIs returned unauthorized at all envs
* [MDEXP-365](https://issues.folio.org/browse/MDEXP-365) - Fix generated records on the fly for default profile
* [MDEXP-367](https://issues.folio.org/browse/MDEXP-367) - Provide granular error in the error log so that the same information is not repeated multiple times
* [MDEXP-377](https://issues.folio.org/browse/MDEXP-377) - Job completed with errors does not show how many records failed3
* [MDEXP-310](https://issues.folio.org/browse/MDEXP-310) - Delete file in S3

## 11/13/2020 v3.0.4 Released
This is a bugfix release for fixing the wrong job execution state if upload an empty file, that breaks UI

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v3.0.3...v3.0.4)

### Bug Fixes
* [MDEXP-338](https://issues.folio.org/browse/MDEXP-338) - Fix periodically wrong jobExecution state when uploading an empty file

## 11/12/2020 v3.0.3 Released
This is a bugfix release for fixing the job execution status when invalid UUIDs are present in the batch

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v3.0.2...v3.0.3)

### Bug Fixes
* [MDEXP-337](https://issues.folio.org/browse/MDEXP-337) - Fix wrong jobExecution status when some UUIDs are invalid or not found in storages not in last batch

## 11/09/2020 v3.0.2 Released
This is a bugfix release for correcting SQL statement name in the migration script.

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v3.0.1...v3.0.2)

## 11/07/2020 v3.0.1 Released
This hotfix release includes fixes to handle csv file with different symbols, handle empty csv file correctly,
displaying UUIDs of records from inventory or SRS that were not found in error logs,fixed missing $ 3 subfield in MARC
file and duplication of multiple error log entries. Also, this release includes migration script for 
mapping profiles.

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v3.0.0...v3.0.1)

### Bug Fixes
* [MDEXP-316](https://issues.folio.org/browse/MDEXP-316) - Error logs does not contains items records if mapping process is failed
* [MDEXP-314](https://issues.folio.org/browse/MDEXP-314) - Mapping profile migrate fieldIds
* [MDEXP-313](https://issues.folio.org/browse/MDEXP-313) - Failed job associated with generated mrc file and contains multiple entries in the error log
* [MDEXP-311](https://issues.folio.org/browse/MDEXP-311) - Wrong job status when all UUIDs in the last batch not present in storages
* [MDEXP-309](https://issues.folio.org/browse/MDEXP-309) - When UUID is not found the error log doesn't list it
* [MDEXP-308](https://issues.folio.org/browse/MDEXP-308) - Subfield $3 is missing for the MARC tags with item data
* [MDEXP-307](https://issues.folio.org/browse/MDEXP-307) - Fix security dependency issue
* [MDEXP-305](https://issues.folio.org/browse/MDEXP-305) - Investigate if generated mrc files and directories are not being deleted
* [MDEXP-267](https://issues.folio.org/browse/MDEXP-267) - After uploading of empty file the progress field is not provided
* [MDEXP-176](https://issues.folio.org/browse/MDEXP-176) - Handle csv files with other characters

## 10/14/2020 v3.0.0 Released
 Major version release which includes below features :
 * specify profile transformations for multiple fields from instance,holdings,items
 * expire long running/stuck jobs
 * ability to look at logs for each job-execution, and delete jobs
 * exporting MARC records using a CQL query

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v2.1.4...v3.0.0)

### Stories
* [MDEXP-300](https://issues.folio.org/browse/MDEXP-300) - Close ASW client after getting download link
* [MDEXP-294](https://issues.folio.org/browse/MDEXP-294) - Export records using CQL
* [MDEXP-288](https://issues.folio.org/browse/MDEXP-288) - Final verification migration scripts before release- Q32020
* [MDEXP-282](https://issues.folio.org/browse/MDEXP-282) - Observations on testing data-export with profile
* [MDEXP-281](https://issues.folio.org/browse/MDEXP-281) - POC: Export records using CQL
* [MDEXP-277](https://issues.folio.org/browse/MDEXP-277) - POC: Investigate making fewer calls to Inventory modules
* [MDEXP-271](https://issues.folio.org/browse/MDEXP-271) - Differentiate "displayNameKey" for entities without "referenceDataValue"
* [MDEXP-270](https://issues.folio.org/browse/MDEXP-270) - Review state control of module
* [MDEXP-266](https://issues.folio.org/browse/MDEXP-266) - Generate MARC record- Holdings and items in same field
* [MDEXP-264](https://issues.folio.org/browse/MDEXP-264) - API endpoints to follow naming conventions
* [MDEXP-263](https://issues.folio.org/browse/MDEXP-263) - API for DELETE /job-executions/{jobExecutionId}
* [MDEXP-261](https://issues.folio.org/browse/MDEXP-261) - Create API GET/data-export/logs for error log handling
* [MDEXP-259](https://issues.folio.org/browse/MDEXP-259) - Log errors for Holdings records export
* [MDEXP-257](https://issues.folio.org/browse/MDEXP-257) - Log errors for inventory Instance record exports
* [MDEXP-256](https://issues.folio.org/browse/MDEXP-256) - Modify status of the completed export job
* [MDEXP-255](https://issues.folio.org/browse/MDEXP-255) - Generate-marc-utils: add default support for translations
* [MDEXP-252](https://issues.folio.org/browse/MDEXP-252) - Applying mapping profile - location fields - holdings and item
* [MDEXP-251](https://issues.folio.org/browse/MDEXP-251) - Mapping profile - provide field name - locations - holdings and items
* [MDEXP-249](https://issues.folio.org/browse/MDEXP-249) - Applying mapping profile - Item - effective call number
* [MDEXP-248](https://issues.folio.org/browse/MDEXP-248) - Mapping profile - provide field name - item - effective call number
* [MDEXP-246](https://issues.folio.org/browse/MDEXP-246) - Implement Expire stuck jobs
* [MDEXP-243](https://issues.folio.org/browse/MDEXP-243) - Applying mapping profile - Combination of record types - selected fields - missing transformation
* [MDEXP-238](https://issues.folio.org/browse/MDEXP-238) - Applying mapping profile - Item - selected fields with transformation provided
* [MDEXP-237](https://issues.folio.org/browse/MDEXP-237) - Applying mapping profile - Holdings - selected fields with transformation provided
* [MDEXP-236](https://issues.folio.org/browse/MDEXP-236) - Check how much memory the module needs to get generated 1 file
* [MDEXP-233](https://issues.folio.org/browse/MDEXP-233) - Apply sorting for mapping transformations subfields in alphabetical order
* [MDEXP-231](https://issues.folio.org/browse/MDEXP-231) - Mapping profile - provide field name - holdings statement
* [MDEXP-230](https://issues.folio.org/browse/MDEXP-230) - Mapping profile - provide field name - notes (holdings and items)
* [MDEXP-229](https://issues.folio.org/browse/MDEXP-229) - Mapping profile - provide field name - electronic access
* [MDEXP-227](https://issues.folio.org/browse/MDEXP-227) - Mapping profile - provide field name - contributors
* [MDEXP-225](https://issues.folio.org/browse/MDEXP-225) - Restrict deletion of default mapping profile and job profile
* [MDEXP-218](https://issues.folio.org/browse/MDEXP-218) - Mapping profile - provide items field names - field value
* [MDEXP-217](https://issues.folio.org/browse/MDEXP-217) - Mapping profile - provide field name - value for key - with reference data
* [MDEXP-216](https://issues.folio.org/browse/MDEXP-216) - Mapping profile - provide holdings field names - field value
* [MDEXP-211](https://issues.folio.org/browse/MDEXP-211) - Mod-data-export migrate to Java 11
* [MDEXP-210](https://issues.folio.org/browse/MDEXP-210) - Mapping profile - provide instance field names - field value
* [MDEXP-207](https://issues.folio.org/browse/MDEXP-207) - Performance improvements
* [MDEXP-174](https://issues.folio.org/browse/MDEXP-174) - Use the created shared library
* [MDEXP-150](https://issues.folio.org/browse/MDEXP-150) - Mapping profile - transformations - retrieve field names
* [MDEXP-141](https://issues.folio.org/browse/MDEXP-141) - Applying mapping profile - Instance - selected fields with transformation provided
* [MDEXP-90](https://issues.folio.org/browse/MDEXP-90) - POC: Make data-export horizontally scalable
* [MDEXP-75](https://issues.folio.org/browse/MDEXP-75) - Create a Test suite

### Bug Fixes
* [MDEXP-306](https://issues.folio.org/browse/MDEXP-306) - Fix migration script for handling statuses
* [MDEXP-304](https://issues.folio.org/browse/MDEXP-304) - Fix module logging after upgrade to java 11
* [MDEXP-301](https://issues.folio.org/browse/MDEXP-301) - Fix NPE while export with material type field for custom mapping profile
* [MDEXP-279](https://issues.folio.org/browse/MDEXP-279) - Mapping materialTypeId and permanentLoanTypeId to proper fields
* [MDEXP-244](https://issues.folio.org/browse/MDEXP-244) - Not all items displayed if mapped to the MARC subfield


## 07/29/2020 v2.1.4 Released
This is a bugfix release to address changes missing metadata for default job profiles and mapping profiles

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v2.1.3...v2.1.4)

### Bug Fixes
* [MDEXP-253](https://issues.folio.org/browse/MDEXP-253) - Default Profiles do not have metadata causing error screen

## 07/28/2020 v2.1.3 Released
This is a bugfix release to address upgrade issues from v1.1.1 to current version because of the migration scripts

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v2.1.2...v2.1.3)

### Bug Fixes
* [MDEXP-247](https://issues.folio.org/browse/MDEXP-247) - Data Migration issue on goldenrod


## 07/13/2020 v2.1.2 Released
This bugfix release includes fixes to concurrency issues while exporting jobs and also fixes empty query sent to 
inventory which was causing un responsive module

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v2.1.1...v2.1.2)

### Bug Fixes
* [MDEXP-220](https://issues.folio.org/browse/MDEXP-220) - Inconsistent results when exporting the same dataset with default mapping profile
* [MDEXP-219](https://issues.folio.org/browse/MDEXP-219) - Export job with custom mapping profile gets stuck when retrieving data from inventory


## 07/09/2020 v2.1.1 Released
This release includes minor bug fixes related to job profile name and minor performance improvement for generating instances on the fly
 
[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v2.1.0...v2.1.1)

### Stories
* [MDEXP-166](https://issues.folio.org/browse/MDEXP-166) - Make records processing parallel

### Bug Fixes
* [MDEXP-214](https://issues.folio.org/browse/MDEXP-214) - Incorrect indicators for Corporate and Meeting primary contributors
* [MDEXP-208](https://issues.folio.org/browse/MDEXP-208) - JobProfileName field not populating while getting job execution
* [MDEXP-203](https://issues.folio.org/browse/MDEXP-203) - Incorrect number of errors reported


## 06/25/2020 v2.1.0 Released
The major change in this release is the interface dependency change to "source-storage-source-records" for performance reasons.
Also other minor changes include jobprofile association with job execution and MARC field combining on Transformations
 
[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v2.0.0...v2.1.0)

### Stories
* [MDEXP-199](https://issues.folio.org/browse/MDEXP-199) - Update dependency on SRS v4.0.0
* [MDEXP-194](https://issues.folio.org/browse/MDEXP-194) - Provide information for UI about the job profile associated with the executed job
* [MDEXP-193](https://issues.folio.org/browse/MDEXP-193) - Mapping profile - combine subfields in one MARC field



### Bug Fixes
* [MDEXP-200](https://issues.folio.org/browse/MDEXP-200) - GoldenRod upgrade issues on jobExecution table
* [MDEXP-186](https://issues.folio.org/browse/MDEXP-186) - Invalid fileDefinition returns success for /export

## 06/12/2020 v2.0.0 Released
The major changes in this release include, generating MARC records on the fly for Instances, being able to define mapping profiles with 
transformations for holdings and items, and corresponding job profiles to trigger export job. There is also ability to transform existing MARC records
on the fly by specifying transformations

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v1.1.2...v2.0.0)

### Stories
* [MDEXP-179](https://issues.folio.org/browse/MDEXP-179) - Final verification migration scripts before release Q2 2020
* [MDEXP-177](https://issues.folio.org/browse/MDEXP-177) - Generate MARC bib record - 653$a/Subjects
* [MDEXP-175](https://issues.folio.org/browse/MDEXP-175) - Add user details in mapping profiles/job profiles
* [MDEXP-172](https://issues.folio.org/browse/MDEXP-172) - Job profile - create a default job profile
* [MDEXP-171](https://issues.folio.org/browse/MDEXP-171) - Use specified job profile in /export endpoint
* [MDEXP-169](https://issues.folio.org/browse/MDEXP-169) - Tenant level mapping rules
* [MDEXP-165](https://issues.folio.org/browse/MDEXP-165) - Mapping profile - transformations - traversing multiple holdings and items
* [MDEXP-162](https://issues.folio.org/browse/MDEXP-162) - Mapping profile - transformations - process holdings and items simple fields
* [MDEXP-151](https://issues.folio.org/browse/MDEXP-151) - Mapping profile - transformations - process holdings permanent/temporary locations
* [MDEXP-143](https://issues.folio.org/browse/MDEXP-143) - Create Mapping profile and Job profile schemas
* [MDEXP-138](https://issues.folio.org/browse/MDEXP-138) - Applying mapping profile - transformations are provided for all selected fields
* [MDEXP-137](https://issues.folio.org/browse/MDEXP-137) - Mapping profiles APIs
* [MDEXP-136](https://issues.folio.org/browse/MDEXP-136) - Generate MARC bib record - 008 fields
* [MDEXP-135](https://issues.folio.org/browse/MDEXP-135) - Create API test for data-export/export endpoint
* [MDEXP-133](https://issues.folio.org/browse/MDEXP-133) - Get the number of already processed records (successfully and failed) for a given job
* [MDEXP-131](https://issues.folio.org/browse/MDEXP-131) - Mapping profile - create default mapping profile
* [MDEXP-126](https://issues.folio.org/browse/MDEXP-126) - Generate MARC bib record - 300 field
* [MDEXP-125](https://issues.folio.org/browse/MDEXP-125) - Generate MARC bib record - 490$a/series
* [MDEXP-123](https://issues.folio.org/browse/MDEXP-123) - Generate MARC bib record - 264/publication data
* [MDEXP-122](https://issues.folio.org/browse/MDEXP-122) - Generate MARC bib record - 005 field
* [MDEXP-115](https://issues.folio.org/browse/MDEXP-115) - Rewrite unit test
* [MDEXP-103](https://issues.folio.org/browse/MDEXP-103) - Generate MARC bib record - Contributors - conditionally replacing keys with values
* [MDEXP-102](https://issues.folio.org/browse/MDEXP-102) - Generate MARC bib record - 655 $a/natureOfContentId - replacing keys with values
* [MDEXP-98](https://issues.folio.org/browse/MDEXP-98) - Generate MARC bib - Leader
* [MDEXP-97](https://issues.folio.org/browse/MDEXP-97) - Generate MARC bib record - Identifiers - conditionally replacing keys with values
* [MDEXP-93](https://issues.folio.org/browse/MDEXP-93) - Generate MARC bib record containing 001, 245$a and 999ff$i fields
* [MDEXP-92](https://issues.folio.org/browse/MDEXP-92) - Revisit naming convention of the file generated by export
* [MDEXP-78](https://issues.folio.org/browse/MDEXP-78) - Make /data-export/fileDefinitions/{{fileDefinitionId}}/upload idempotent
* [MDEXP-77](https://issues.folio.org/browse/MDEXP-77) - Refactor /data-export/export
* [MDEXP-37](https://issues.folio.org/browse/MDEXP-37) - RecordLoaderService: retrieve Inventory instance records without underlying SRS record

### Bug Fixes
* [MDEXP-189](https://issues.folio.org/browse/MDEXP-189) - Export fails with NPE when dataOfPublication is null
* [MDEXP-149](https://issues.folio.org/browse/MDEXP-149) - Instances with underlying SRS should not have MARC bib generated on the fly
* [MDEXP-148](https://issues.folio.org/browse/MDEXP-148) - Incorrect record count for the records generated on the fly
* [MDEXP-139](https://issues.folio.org/browse/MDEXP-139) - Extended character set incorrectly encoded


## 04/02/2020 v1.1.1 - Released
This release contains a bugfix to the fromModuleVersion and start of sequence to 1

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v1.1.0...v1.1.1)

### Bug Fixes
* [MDEXP-118](https://issues.folio.org/browse/MDEXP-118) - fromModuleVersion not updated

## 04/01/2020 v1.1.0 - Released
This release contains few bugfixes and also populates few fields that the required to be shown on the UI

[Full Changelog](https://github.com/folio-org/mod-data-export/compare/v1.0.0...v1.1.0)

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


## 03/13/2020 v1.0.0 - Released

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
