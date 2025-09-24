package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.ContributorNameTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for retrieving contributor name types.
 */
@FeignClient(name = "contributor-name-types")
public interface ContributorNameTypesClient {
  /**
   * Retrieves contributor name types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of contributor name types
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  ContributorNameTypes getContributorNameTypes(@RequestParam long limit);
}
