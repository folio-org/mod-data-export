package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.Locations;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving locations. */
@HttpExchange(url = "locations")
public interface LocationsClient {
  /**
   * Retrieves locations with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of locations
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  Locations getLocations(@RequestParam long limit);
}
