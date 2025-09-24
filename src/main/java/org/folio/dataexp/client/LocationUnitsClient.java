package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.Campuses;
import org.folio.dataexp.domain.dto.Institutions;
import org.folio.dataexp.domain.dto.Libraries;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for retrieving location units such as campuses, institutions, and libraries.
 */
@FeignClient(name = "location-units")
public interface LocationUnitsClient {
  /**
   * Retrieves campuses with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of campuses
   */
  @GetMapping(value = "/campuses", produces = APPLICATION_JSON_VALUE)
  Campuses getCampuses(@RequestParam long limit);

  /**
   * Retrieves institutions with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of institutions
   */
  @GetMapping(value = "/institutions", produces = APPLICATION_JSON_VALUE)
  Institutions getInstitutions(@RequestParam long limit);

  /**
   * Retrieves libraries with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of libraries
   */
  @GetMapping(value = "/libraries", produces = APPLICATION_JSON_VALUE)
  Libraries getLibraries(@RequestParam long limit);
}
