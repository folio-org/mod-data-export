package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.ElectronicAccessRelationships;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving electronic access relationships. */
@HttpExchange(url = "electronic-access-relationships")
public interface ElectronicAccessRelationshipsClient {
  /**
   * Retrieves electronic access relationships with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of electronic access relationships
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  ElectronicAccessRelationships getElectronicAccessRelationships(@RequestParam long limit);
}
