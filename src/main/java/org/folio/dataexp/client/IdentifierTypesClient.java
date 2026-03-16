package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.IdentifierTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving identifier types. */
@HttpExchange(url = "identifier-types")
public interface IdentifierTypesClient {
  /**
   * Retrieves identifier types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of identifier types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  IdentifierTypes getIdentifierTypes(@RequestParam long limit);
}
