package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.IssuanceModes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving modes of issuance. */
@HttpExchange(url = "modes-of-issuance")
public interface IssuanceModesClient {
  /**
   * Retrieves issuance modes with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of issuance modes
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  IssuanceModes getIssuanceModes(@RequestParam long limit);
}
