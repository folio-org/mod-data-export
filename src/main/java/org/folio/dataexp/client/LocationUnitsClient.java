package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.Campuses;
import org.folio.dataexp.domain.dto.Institutions;
import org.folio.dataexp.domain.dto.Libraries;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving location units such as campuses, institutions, and libraries. */
@HttpExchange(url = "location-units")
public interface LocationUnitsClient {
  /**
   * Retrieves campuses with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of campuses
   */
  @GetExchange(value = "/campuses", accept = APPLICATION_JSON_VALUE)
  Campuses getCampuses(@RequestParam long limit);

  /**
   * Retrieves institutions with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of institutions
   */
  @GetExchange(value = "/institutions", accept = APPLICATION_JSON_VALUE)
  Institutions getInstitutions(@RequestParam long limit);

  /**
   * Retrieves libraries with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of libraries
   */
  @GetExchange(value = "/libraries", accept = APPLICATION_JSON_VALUE)
  Libraries getLibraries(@RequestParam long limit);
}
