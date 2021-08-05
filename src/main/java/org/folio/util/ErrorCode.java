package org.folio.util;

import org.folio.rest.jaxrs.model.Error;

import java.util.ArrayList;
import java.util.List;

public enum ErrorCode {

  GENERIC_ERROR_CODE("error.genericError", "Generic error"),
  FILE_DEFINITION_NOT_FOUND("error.fileDefinition.notFound", "File Definition not found"),
  S3_BUCKET_NAME_NOT_FOUND("error.s3.bucketNameNotFound", "S3 bucket name is not found in System Properties"),
  NO_FILE_GENERATED("error.binaryFile.notGenerated", "Nothing to export: no binary file generated"),
  USER_NOT_FOUND("error.user.notFound", "User not found"),
  INVALID_UPLOADED_FILE_EXTENSION("error.uploadedFile.invalidExtension", "File name extension does not corresponds csv format"),
  FILE_ALREADY_UPLOADED("error.fileDefinition.fileAlreadyUploaded", "File already uploaded for this FileDefinition"),
  NOTHING_TO_EXPORT("error.nothingToExport", "No exported records, nothing to export"),
  FAIL_TO_UPDATE_JOB("error.jobExecution.failToUpdateJob", "Fail to prepare job execution for export"),
  ERROR_QUERY_RULES_FROM_CONFIGURATIONS("error.configurations.queryRules", "Error while query the rules from mod configuration: %s"),
  ERROR_QUERY_CONFIGURATIONS("error.configurations.queryConfigs", "Error while query the configs from mod configuration by query: %s, %s"),
  ERROR_QUERY_HOST("error.configurations.queryHost", "No configuration for host found in mod-config. There will be no links to the failed records for this job"),
  ERROR_GETTING_INSTANCES_BY_IDS("error.inventory.gettingInstancesByIds", "Error while getting instances by ids. %s"),
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
  SOME_UUIDS_NOT_FOUND("error.uuidsNotFound", "UUIDs not found in SRS or inventory: %s"),
  SOME_RECORDS_FAILED("error.someRecordsFailed", "Export is completed with errors, some records have failed to export, number of failed records: %s"),
  DATE_PARSE_ERROR_CODE("error.mapping.dateParse", "An error occurs during parsing the date while the mapping process"),
  UNDEFINED("error.mapping.undefined", "Undefined error during the mapping process"),
  ERROR_FIELDS_MAPPING_INVENTORY("error.mapping.fieldsMappingInventory", "An error occurred during fields mapping for inventory record"),
  ERROR_FIELDS_MAPPING_INVENTORY_WITH_REASON("error.mapping.fieldsMappingInventoryWithReason", "An error occurred during fields mapping for inventory record, affected field: fieldName - %s, fieldValue - $s"),
  ERROR_FIELDS_MAPPING_SRS("error.mapping.fieldsMappingSrs", "An error occurred during fields mapping for srs record with id: %s, reason: %s, cause: %s"),
  ERROR_MESSAGE_PLACEHOLDER("error.messagePlaceholder", "%s"),
  ERROR_MARC_RECORD_CANNOT_BE_CONVERTED("error.messagePlaceholder", "Json record cannot be converted to marc format, cause: %s"),
  INVALID_SRS_MAPPING_PROFILE_RECORD_TYPE("error.mappingProfile.invalidSrsRecordTypeCombination", "SRS record type cannot be combined together with INSTANCE record type");

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
    return new Error().withCode(code).withMessage(description);
  }

  public static List<String> errorCodesAccordingToExport() {
    List<String> errorCodesForUUIDs = new ArrayList<>();
    errorCodesForUUIDs.add(SOME_UUIDS_NOT_FOUND.getCode());
    errorCodesForUUIDs.add(SOME_RECORDS_FAILED.getCode());
    errorCodesForUUIDs.add(INVALID_UUID_FORMAT.getCode());
    errorCodesForUUIDs.add(DATE_PARSE_ERROR_CODE.getCode());
    errorCodesForUUIDs.add(ERROR_FIELDS_MAPPING_INVENTORY_WITH_REASON.getCode());
    errorCodesForUUIDs.add(ERROR_FIELDS_MAPPING_SRS.getCode());
    errorCodesForUUIDs.add(UNDEFINED.getCode());
    return errorCodesForUUIDs;
  }
}
