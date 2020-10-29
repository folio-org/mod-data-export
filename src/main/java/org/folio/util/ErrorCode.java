package org.folio.util;

import org.folio.rest.jaxrs.model.Error;

public enum ErrorCode {

  GENERIC_ERROR_CODE("genericError", "Generic error"),
  FILE_DEFINITION_NOT_FOUND("fileDefinitionNotFound", "File Definition not found"),
  S3_BUCKET_NOT_PROVIDED("bucketNotProvided", "S3 bucket name is not found in System Properties"),
  NO_FILE_GENERATED("noFileGenerated", "Nothing to export: no binary file generated"),
  USER_NOT_FOUND("userNotFound", "User not found"),
  INVALID_UPLOADED_FILE_EXTENSION("invalidUploadedFileExtension", "File name extension does not corresponds csv format"),
  FILE_ALREADY_UPLOADED("fileAlreadyUploaded","File already uploaded for this FileDefinition"),
  NOTHING_TO_EXPORT("nothingToExport", "No exported records, nothing to export"),
  SOME_RECORDS_FAILED("someRecordsFailed", "Export is finished with errors, some records are failed to export, number of failed records: "),
  SOME_UUIDS_NOT_FOUND("someUUIDsNotFound", "UUIDs not found in SRS or inventory: ");

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
}
