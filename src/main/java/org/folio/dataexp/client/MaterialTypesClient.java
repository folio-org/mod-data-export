package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.MaterialTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "material-types")
public interface MaterialTypesClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  MaterialTypes getMaterialTypes(@RequestParam long limit);
}
