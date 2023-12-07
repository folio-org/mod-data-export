package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.IssuanceModes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "modes-of-issuance")
public interface IssuanceModesClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  IssuanceModes getIssuanceModes(@RequestParam long limit);
}
