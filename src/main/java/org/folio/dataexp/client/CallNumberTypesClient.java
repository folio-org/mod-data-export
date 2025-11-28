package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.CallNumberTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving call number types. */
@FeignClient(name = "call-number-types")
public interface CallNumberTypesClient {
  /**
   * Retrieves call number types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of call number types
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  CallNumberTypes getCallNumberTypes(@RequestParam long limit);
}
