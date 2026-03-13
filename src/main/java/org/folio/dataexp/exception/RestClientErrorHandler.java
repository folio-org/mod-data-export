package org.folio.dataexp.exception;

import java.io.IOException;

import org.folio.spring.exception.NotFoundException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

@Component
public class RestClientErrorHandler {

  private final DefaultResponseErrorHandler defaultErrorHandler = new DefaultResponseErrorHandler();

  public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
    int status = response.getStatusCode().value();

    if (status == 404) {
      throw new NotFoundException("Unable to find");
    } else {
      defaultErrorHandler.handleError(request.getURI(), request.getMethod(), response);
    }
  }
}
