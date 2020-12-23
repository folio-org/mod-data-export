package org.folio.util;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import io.vertx.core.Promise;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.tools.utils.ValidationHelper;

public final class ExceptionToResponseMapper {

  private static final Logger LOGGER = LogManager.getLogger(ExceptionToResponseMapper.class);

  private ExceptionToResponseMapper() {
  }

  public static Response map(Throwable throwable) {
    final Error error;
    final int code;

    if (throwable instanceof NotFoundException) {
      return Response.status(NOT_FOUND.getStatusCode())
        .type(MediaType.TEXT_PLAIN)
        .entity(throwable.getMessage())
        .build();
    }


    if (throwable instanceof BadRequestException) {
      return Response.status(BAD_REQUEST.getStatusCode())
        .type(MediaType.TEXT_PLAIN)
        .entity(throwable.getMessage())
        .build();
    }

    if (throwable instanceof ServiceException) {
      code = ((ServiceException) throwable).getCode();
      error = ((ServiceException) throwable).getError();

      return Response.status(code)
          .type(MediaType.TEXT_PLAIN)
          .entity(error.getMessage())
          .build();
    }


    Promise<Response> validationFuture = Promise.promise();
    ValidationHelper.handleError(throwable, validationFuture);
    if (validationFuture.future().isComplete()) {
      Response response = validationFuture.future().result();
      if (response.getStatus() == INTERNAL_SERVER_ERROR.getStatusCode()) {
        LOGGER.error(throwable.getMessage(), throwable);
      }
      return response;
    }
    LOGGER.error(throwable.getMessage(), throwable);
    return Response.status(INTERNAL_SERVER_ERROR.getStatusCode())
      .type(MediaType.TEXT_PLAIN)
      .entity(INTERNAL_SERVER_ERROR.getReasonPhrase())
      .build();
  }
}
