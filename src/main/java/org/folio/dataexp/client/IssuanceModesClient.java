package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.IssuanceModes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving modes of issuance. */
@FeignClient(name = "modes-of-issuance")
public interface IssuanceModesClient {
  /**
   * Retrieves issuance modes with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of issuance modes
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  IssuanceModes getIssuanceModes(@RequestParam long limit);
}
