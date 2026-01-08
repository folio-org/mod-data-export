package org.folio.dataexp.util;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

/** Provide alternate to default Feign retryable error decoding */
public class FqmErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    FeignException exception = feign.FeignException.errorStatus(methodKey, response);
    int status = response.status();
    if (status >= 500) {
      return new RetryableException(
        status,
        exception.getMessage(),
        response.request().httpMethod(),
        exception,
        null,
        response.request()
      );
    }
    return exception;
  }

}
