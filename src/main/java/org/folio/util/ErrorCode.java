package org.folio.util;

import org.folio.rest.jaxrs.model.Error;

import java.util.ArrayList;
import java.util.List;

public enum ErrorCode {

  GENERIC_ERROR_CODE("genericError", "Generic error"),
  FILE_DEFINITION_NOT_FOUND("fileDefinitionNotFound", "File Definition not found"),
  S3_BUCKET_NOT_PROVIDED("bucketNotProvided", "S3 bucket name is not found in System Properties"),
  NO_FILE_GENERATED("noFileGenerated", "Nothing to export: no binary file generated"),
  USER_NOT_FOUND("userNotFound", "User not found"),
  INVALID_UPLOADED_FILE_EXTENSION("invalidUploadedFileExtension", "File name extension does not corresponds csv format"),
  FILE_ALREADY_UPLOADED("fileAlreadyUploaded","File already uploaded for this FileDefinition"),
  NOTHING_TO_EXPORT("nothingToExport", "No exported records, nothing to export"),
  SOME_RECORDS_FAILED("someRecordsFailed", "Export is completed with errors, some records have failed to export, number of failed records: "),
  SOME_UUIDS_NOT_FOUND("someUUIDsNotFound", "UUIDs not found in SRS or inventory: "),
  INVALID_UUID_FORMAT("invalidUUIDFormat", "Invalid UUID format: "),
  INVALID_EXPORT_FILE_DEFINITION_ID("invalidExportFileDefinitionId", "Invalid export file definition id: "),
  FAIL_TO_UPDATE_JOB("failToUpdateJob", "Fail to prepare job execution for export");
  DEFAULT_MAPPING_PROFILE_NOT_FOUND("defaultMappingProfileNotFound", "Default mapping profile not found");

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

  public static List<String> reasonsAccordingToUUIDs() {
    List<String> errorCodesForUUIDs = new ArrayList<>();
    errorCodesForUUIDs.add(SOME_UUIDS_NOT_FOUND.getDescription());
    errorCodesForUUIDs.add(SOME_RECORDS_FAILED.getDescription());
    errorCodesForUUIDs.add(INVALID_UUID_FORMAT.getDescription());
    return errorCodesForUUIDs;
  }
}
