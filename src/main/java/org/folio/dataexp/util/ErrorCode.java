package org.folio.dataexp.util;

import org.folio.dataexp.domain.dto.Error;

import java.util.ArrayList;
import java.util.List;

import static org.folio.dataexp.util.Constants.ERROR_MESSAGE_PLACEHOLDER_CODE;
import static org.folio.dataexp.util.Constants.INVALID_EXTENSION_ERROR_CODE;

public enum ErrorCode {

  GENERIC_ERROR_CODE("error.genericError", "Generic error"),
  FILE_DEFINITION_NOT_FOUND("error.fileDefinition.notFound", "File Definition not found"),
  S3_BUCKET_NAME_NOT_FOUND("error.s3.bucketNameNotFound", "S3 bucket name is not found in System Properties"),
  NO_FILE_GENERATED("error.binaryFile.notGenerated", "Nothing to export: no binary file generated"),
  USER_NOT_FOUND("error.user.notFound", "User not found"),
  INVALID_UPLOADED_FILE_EXTENSION(INVALID_EXTENSION_ERROR_CODE, "File name extension does not corresponds csv format"),
  INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE(INVALID_EXTENSION_ERROR_CODE, "Only csv format is supported for holdings export"),
  INVALID_UPLOADED_FILE_EXTENSION_FOR_AUTHORITY_ID_TYPE(INVALID_EXTENSION_ERROR_CODE, "Only csv format is supported for authority export"),
  FILE_ALREADY_UPLOADED("error.fileDefinition.fileAlreadyUploaded", "File already uploaded for this FileDefinition"),
  NOTHING_TO_EXPORT("error.nothingToExport", "No exported records, nothing to export"),
  FAIL_TO_UPDATE_JOB("error.jobExecution.failToUpdateJob", "Fail to prepare job execution for export"),
  ERROR_QUERY_RULES_FROM_CONFIGURATIONS("error.configurations.queryRules", "Error while query the rules from mod configuration: %s"),
  ERROR_QUERY_CONFIGURATIONS("error.configurations.queryConfigs", "Error while query the configs from mod configuration by query: %s, %s"),
  ERROR_QUERY_HOST("error.configurations.queryHost", "No configuration for host found in mod-config. There will be no links to the failed records for this job"),
  ERROR_GETTING_INSTANCES_BY_IDS("error.inventory.gettingInstancesByIds", "Error while getting instances by ids. %s"),
  ERROR_GETTING_HOLDINGS_BY_IDS(ERROR_MESSAGE_PLACEHOLDER_CODE, "Error while getting holdings by ids. %s"),
  ERROR_GETTING_REFERENCE_DATA("error.inventory.gettingReferenceData  ", "Error while getting reference data from inventory during the export process by calling %s"),
  ERROR_GETTING_HOLDINGS_BY_INSTANCE_ID("error.inventory.gettingHoldingsByInstanceId", "Error while getting holdings by instance id: %s, message: %s"),
  ERROR_GETTING_ITEM_BY_HOLDINGS_ID("error.inventory.gettingItemsByHoldingsId", "Error while getting items by holding ids %s"),
  ERROR_CALLING_URI("error.callingUri", "Exception while calling %s, message: %s"),
  ERROR_GETTING_USER("error.user.gettingById", "Error while getting user with id = %s, message: %s"),
  ERROR_SAVING_RECORD_TO_FILE("error.savingRecordToFile", "Error during saving record to file"),
  INVALID_EXPORT_FILE_DEFINITION_ID("error.fileDefinition.invalidWithId", "Invalid export file definition with id: %s"),
  INVALID_EXPORT_FILE_DEFINITION("error.fileDefinition.invalid", "Invalid export file definition"),
  S3_BUCKET_IS_NOT_PROVIDED("error.s3.bucketIsNotProvided", "S3 bucket is not provided"),
  INVALID_UUID_FORMAT("error.invalidUuidFormat", "Invalid UUID format: %s"),
  DEFAULT_MAPPING_PROFILE_NOT_FOUND("error.mappingProfile.defaultNotFound", "Default mapping profile not found"),
  ERROR_READING_FROM_INPUT_FILE("error.readingFromInputFile", "Error while reading from input file with uuids or file is empty"),
  ERROR_INVALID_CQL_SYNTAX("error.invalidCqlSyntax", "Invalid CQL syntax in %s"),
  SOME_UUIDS_NOT_FOUND("error.uuidsNotFound", "UUIDs not found in SRS or inventory: %s"),
  SOME_RECORDS_FAILED("error.someRecordsFailed", "Export is completed with errors, some records have failed to export, number of failed records: %s"),
  DATE_PARSE_ERROR_CODE("error.mapping.dateParse", "An error occurs during parsing the date while the mapping process"),
  UNDEFINED("error.mapping.undefined", "Undefined error during the mapping process"),
  ERROR_FIELDS_MAPPING_INVENTORY("error.mapping.fieldsMappingInventory", "An error occurred during fields mapping for inventory record"),
  ERROR_FIELDS_MAPPING_INVENTORY_WITH_REASON("error.mapping.fieldsMappingInventoryWithReason", "An error occurred during fields mapping for inventory record, affected field: fieldName - %s, fieldValue - $s"),
  ERROR_FIELDS_MAPPING_SRS("error.mapping.fieldsMappingSrs", "An error occurred during fields mapping for srs record with id: %s, reason: %s, cause: %s"),
  ERROR_MESSAGE_PLACEHOLDER(ERROR_MESSAGE_PLACEHOLDER_CODE, "%s"),
  ERROR_MARC_RECORD_CANNOT_BE_CONVERTED(ERROR_MESSAGE_PLACEHOLDER_CODE, "Json record cannot be converted to marc format, cause: %s"),
  INVALID_SRS_MAPPING_PROFILE_RECORD_TYPE("error.mappingProfile.invalidSrsRecordTypeCombination", "SRS record type cannot be combined together with INSTANCE record type"),
  ERROR_ONLY_DEFAULT_HOLDING_JOB_PROFILE_IS_SUPPORTED(ERROR_MESSAGE_PLACEHOLDER_CODE, "For exporting holding records only the default holding job profile is supported"),
  ERROR_ONLY_DEFAULT_AUTHORITY_JOB_PROFILE_IS_SUPPORTED(ERROR_MESSAGE_PLACEHOLDER_CODE, "For exporting authority records only the default authority job profile is supported"),
  ERROR_JOB_IS_EXPIRED(ERROR_MESSAGE_PLACEHOLDER_CODE, "Job was expired: no updates for more than 1 hour"),
  ERROR_FILE_BEING_UPLOADED_IS_TOO_LARGE("error.fileIsTooLarge", "File being uploaded is too large"),
  ERROR_DUPLICATE_SRS_RECORD("error.duplicateSRS", " %s has following SRS records associated: %s"),
  ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC("error.recordIsTooLong", "Record is too long to be a valid MARC binary record, it's length would be %d which is more than 99999 bytes"),
  ERROR_MESSAGE_UUID_IS_SET_TO_DELETION("error.setToDeletion", "%s is set for deletion and cannot be exported using this profile"),
  ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION("error.onlyForSetToDeletion", "This profile can only be used to export authority records set for deletion"),
  ERROR_MESSAGE_PROFILE_USED_ONLY_FOR_NON_DELETED("error.profileUsedToNonDeleted", "This profile can only be used to export authority records not deleted"),
  ERROR_MESSAGE_TENANT_NOT_FOUND_FOR_HOLDING("error.tenantNotFoundForHolding", "Tenant cannot be found for holding with id %s"),
  ERROR_MESSAGE_NO_AFFILIATION("error.noAffiliation", "%s -  the user %s does not have permissions to access the holdings record in %s data tenant.");


  private final String code;
  private final String description;

  ErrorCode(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getCode() {
    return code;
  }

  @Override
  public String toString() {
    return code + ": " + description;
  }

  public Error toError() {
    return new Error().code(code).message(description);
  }

  public static List<String> errorCodesAccordingToExport() {
    List<String> errorCodesForUUIDs = new ArrayList<>();
    errorCodesForUUIDs.add(SOME_UUIDS_NOT_FOUND.getCode());
    errorCodesForUUIDs.add(SOME_RECORDS_FAILED.getCode());
    errorCodesForUUIDs.add(INVALID_UUID_FORMAT.getCode());
    errorCodesForUUIDs.add(DATE_PARSE_ERROR_CODE.getCode());
    errorCodesForUUIDs.add(ERROR_FIELDS_MAPPING_INVENTORY_WITH_REASON.getCode());
    errorCodesForUUIDs.add(ERROR_FIELDS_MAPPING_SRS.getCode());
    errorCodesForUUIDs.add(ERROR_MARC_RECORD_CANNOT_BE_CONVERTED.getCode());
    errorCodesForUUIDs.add(UNDEFINED.getCode());
    errorCodesForUUIDs.add(ERROR_DUPLICATE_SRS_RECORD.getCode());
    errorCodesForUUIDs.add(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode());
    errorCodesForUUIDs.add(ERROR_MESSAGE_UUID_IS_SET_TO_DELETION.getCode());
    errorCodesForUUIDs.add(ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode());
    return errorCodesForUUIDs;
  }
}
