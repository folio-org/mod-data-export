package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;
import org.folio.dataexp.domain.dto.ConsortiumHoldingCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving consortium holdings by instance ID. */
@FeignClient(name = "search/consortium/holdings")
public interface SearchConsortiumHoldings {

  /**
   * Retrieves consortium holdings for a given instance ID.
   *
   * @param instanceId the instance UUID
   * @return a collection of consortium holdings
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  ConsortiumHoldingCollection getHoldingsById(@RequestParam UUID instanceId);
}
