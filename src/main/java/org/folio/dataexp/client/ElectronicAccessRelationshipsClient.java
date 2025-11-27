package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.ElectronicAccessRelationships;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving electronic access relationships. */
@FeignClient(name = "electronic-access-relationships")
public interface ElectronicAccessRelationshipsClient {
  /**
   * Retrieves electronic access relationships with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of electronic access relationships
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  ElectronicAccessRelationships getElectronicAccessRelationships(@RequestParam long limit);
}
