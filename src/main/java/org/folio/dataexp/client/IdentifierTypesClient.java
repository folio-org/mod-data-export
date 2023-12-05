package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.IdentifierTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "identifier-types")
public interface IdentifierTypesClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  IdentifierTypes getIdentifierTypes(@RequestParam long limit);
}
