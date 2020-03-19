package org.folio.util;

import org.folio.rest.jaxrs.model.Error;

public enum ErrorCode {

  GENERIC_ERROR_CODE("genericError", "Generic error"),
  FILE_DEFINITION_NOT_FOUND("fileDefitionNotFound", "File Definition not found"),
  S3_BUCKET_NOT_PROVIDED("bucketNotProvided", "S3 bucket name is not found in System Properties"),
  NO_FILE_GENERATED("noFileGenerated", "Nothing to export: no binary file generated");

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
