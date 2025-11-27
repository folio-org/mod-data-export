package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.IdentifierTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving identifier types. */
@FeignClient(name = "identifier-types")
public interface IdentifierTypesClient {
  /**
   * Retrieves identifier types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of identifier types
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  IdentifierTypes getIdentifierTypes(@RequestParam long limit);
}
