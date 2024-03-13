package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.Campuses;
import org.folio.dataexp.domain.dto.Institutions;
import org.folio.dataexp.domain.dto.Libraries;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "location-units")
public interface LocationUnitsClient {
  @GetMapping(value = "/campuses", produces = APPLICATION_JSON_VALUE)
  Campuses getCampuses(@RequestParam long limit);

  @GetMapping(value = "/institutions", produces = APPLICATION_JSON_VALUE)
  Institutions getInstitutions(@RequestParam long limit);

  @GetMapping(value = "/libraries", produces = APPLICATION_JSON_VALUE)
  Libraries getLibraries(@RequestParam long limit);
}
