package org.folio.clients;

/**
 * This class is created to provide a valid message in clients, with exact URI, and use it for creating an {@link org.folio.rest.jaxrs.model.ErrorLog} object
 */
public class HttpClientException extends Exception {

  public HttpClientException(String message) {
    super(message);
  }

}
