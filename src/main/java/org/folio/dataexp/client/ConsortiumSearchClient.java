package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.Holdings;
import org.folio.dataexp.domain.dto.HoldingsCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "search/consortium")
public interface ConsortiumSearchClient {

  @GetMapping(value = "/holdings", produces = APPLICATION_JSON_VALUE)
  Holdings getHoldingsById(@RequestParam String holdingId);
}
