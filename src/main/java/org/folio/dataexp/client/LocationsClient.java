package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.Locations;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "locations")
public interface LocationsClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  Locations getLocations(@RequestParam long limit);
}
