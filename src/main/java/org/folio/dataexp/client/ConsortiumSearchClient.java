package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.Holdings;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for searching consortium holdings.
 */
@FeignClient(name = "search/consortium")
public interface ConsortiumSearchClient {

  /**
   * Retrieves holdings by holding ID.
   *
   * @param holdingId the holding ID
   * @return the holdings record
   */
  @GetMapping(value = "/holding/{holdingId}", produces = APPLICATION_JSON_VALUE)
  Holdings getHoldingsById(@PathVariable String holdingId);
}
