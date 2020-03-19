package org.folio.rest.exceptions;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.Error;
import org.folio.util.ErrorCode;
import org.folio.HttpStatus;


public class ServiceException extends RuntimeException {
  private static final long serialVersionUID = 8109197948434861504L;

  private final HttpStatus status;
  private ErrorCode errorCode;
  private String message;

  public ServiceException(HttpStatus status, String message) {
    super(StringUtils.isNotEmpty(message) ? message : ErrorCode.GENERIC_ERROR_CODE.getDescription());
    this.status = status;
    this.message = message;
  }

  public ServiceException(HttpStatus status, ErrorCode errCodes) {
    super(errCodes.getDescription());
    this.errorCode = errCodes;
    this.status = status;
  }

  public ServiceException(HttpStatus status) {
    this.status = status;
  }

  public int getCode() {
    return status.toInt();
  }

  public Error getError() {
    return StringUtils.isEmpty(getMessage())
      ? new Error().withCode(errorCode.getCode()).withMessage(errorCode.getDescription())
      : new Error().withCode(String.valueOf(status.toInt())).withMessage(message);
  }

  @Override
  public String getMessage() {
    return message;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
