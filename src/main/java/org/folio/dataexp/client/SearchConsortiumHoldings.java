package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;
import org.folio.dataexp.domain.dto.ConsortiumHoldingCollection;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving consortium holdings by instance ID. */
@HttpExchange(url = "search/consortium/holdings")
public interface SearchConsortiumHoldings {

  /**
   * Retrieves consortium holdings for a given instance ID.
   *
   * @param instanceId the instance UUID
   * @return a collection of consortium holdings
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  ConsortiumHoldingCollection getHoldingsById(@RequestParam UUID instanceId);
}
