package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.MaterialTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving material types. */
@FeignClient(name = "material-types")
public interface MaterialTypesClient {
  /**
   * Retrieves material types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of material types
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  MaterialTypes getMaterialTypes(@RequestParam long limit);
}
