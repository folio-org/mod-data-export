package org.folio.dataexp.config;

import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FeignRetryableErrorDecoderTest {

  private final FeignRetryableErrorDecoder decoder = new FeignRetryableErrorDecoder();

  @ParameterizedTest
  @ValueSource(ints = {429, 502, 503, 504})
  void retriableStatusReturnsRetryableException(int status) {
    var response = buildResponse(status, Map.of());
    var exception = decoder.decode("TestClient#method()", response);
    assertInstanceOf(RetryableException.class, exception);
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 404, 500})
  void nonRetriableStatusReturnsRegularException(int status) {
    var response = buildResponse(status, Map.of());
    var exception = decoder.decode("TestClient#method()", response);
    assertFalse(exception instanceof RetryableException);
  }

  @Test
  void retryAfterHeaderIsRespectedFor429() {
    var headers = Map.<String, Collection<String>>of("Retry-After", List.of("30"));
    var response = buildResponse(429, headers);
    var exception = (RetryableException) decoder.decode("TestClient#method()", response);
    assertNotNull(exception.retryAfter());
  }

  @Test
  void missingRetryAfterHeaderYieldsNullDate() {
    var response = buildResponse(503, Map.of());
    var exception = (RetryableException) decoder.decode("TestClient#method()", response);
    assertNull(exception.retryAfter());
  }

  @Test
  void invalidRetryAfterHeaderYieldsNullDate() {
    var headers = Map.<String, Collection<String>>of("Retry-After", List.of("Wed, 21 Oct 2015 07:28:00 GMT"));
    var response = buildResponse(429, headers);
    var exception = (RetryableException) decoder.decode("TestClient#method()", response);
    assertNull(exception.retryAfter());
  }

  private Response buildResponse(int status, Map<String, Collection<String>> headers) {
    var request = Request.create(Request.HttpMethod.GET, "http://localhost/test",
        Map.of(), null, null, null);
    return Response.builder()
        .status(status)
        .reason("reason")
        .request(request)
        .headers(headers)
        .build();
  }
}
