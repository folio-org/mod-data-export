package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.ContributorNameTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving contributor name types. */
@HttpExchange(url = "contributor-name-types")
public interface ContributorNameTypesClient {
  /**
   * Retrieves contributor name types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of contributor name types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  ContributorNameTypes getContributorNameTypes(@RequestParam long limit);
}
