package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.CallNumberTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving call number types. */
@HttpExchange(url = "call-number-types")
public interface CallNumberTypesClient {
  /**
   * Retrieves call number types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of call number types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  CallNumberTypes getCallNumberTypes(@RequestParam long limit);
}
