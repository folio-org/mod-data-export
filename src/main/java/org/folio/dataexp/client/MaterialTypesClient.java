package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.MaterialTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving material types. */
@HttpExchange(url = "material-types")
public interface MaterialTypesClient {
  /**
   * Retrieves material types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of material types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  MaterialTypes getMaterialTypes(@RequestParam long limit);
}
