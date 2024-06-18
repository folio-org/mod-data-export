package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.Holdings;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "search/consortium")
public interface ConsortiumSearchClient {

  @GetMapping(value = "/holding/{holdingId}", produces = APPLICATION_JSON_VALUE)
  Holdings getHoldingsById(@PathVariable String holdingId);
}
