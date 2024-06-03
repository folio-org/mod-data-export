package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.ConsortiumHoldingCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "search/consortium/holdings")
public interface SearchConsortiumHoldings {

  @GetMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  ConsortiumHoldingCollection getHoldingsById(@RequestParam UUID instanceId);
}
