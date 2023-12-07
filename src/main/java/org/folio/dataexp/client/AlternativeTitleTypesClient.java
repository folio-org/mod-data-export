package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.AlternativeDataTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "alternative-title-types")
public interface AlternativeTitleTypesClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  AlternativeDataTypes getAlternativeTitleTypes(@RequestParam long limit);
}
