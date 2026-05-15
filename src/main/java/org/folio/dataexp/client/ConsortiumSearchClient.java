package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.Holdings;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for searching consortium holdings. */
@HttpExchange(url = "search/consortium")
public interface ConsortiumSearchClient {

  /**
   * Retrieves holdings by holding ID.
   *
   * @param holdingId the holding ID
   * @return the holdings record
   */
  @GetExchange(value = "/holding/{holdingId}", accept = APPLICATION_JSON_VALUE)
  Holdings getHoldingsById(@PathVariable String holdingId);
}
