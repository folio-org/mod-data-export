package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.AlternativeDataTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving alternative title types. */
@HttpExchange(url = "alternative-title-types")
public interface AlternativeTitleTypesClient {
  /**
   * Retrieves alternative title types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of alternative data types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  AlternativeDataTypes getAlternativeTitleTypes(@RequestParam long limit);
}
