package org.folio.dataexp.config;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class FeignRetryableErrorDecoder implements ErrorDecoder {

  private static final Set<Integer> RETRIABLE_STATUSES = Set.of(429, 502, 503, 504);
  private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

  @Override
  public Exception decode(String methodKey, Response response) {
    if (RETRIABLE_STATUSES.contains(response.status())) {
      return new RetryableException(
          response.status(),
          response.reason(),
          response.request().httpMethod(),
          extractRetryAfter(response),
          response.request()
      );
    }
    return defaultDecoder.decode(methodKey, response);
  }

  private Date extractRetryAfter(Response response) {
    var retryAfterValues = response.headers().get("Retry-After");
    if (retryAfterValues == null || retryAfterValues.isEmpty()) {
      return null;
    }
    try {
      long delaySeconds = Long.parseLong(retryAfterValues.iterator().next().trim());
      return new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds));
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
