package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.InstanceTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for retrieving instance types.
 */
@FeignClient(name = "instance-types")
public interface InstanceTypesClient {
  /**
   * Retrieves instance types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of instance types
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  InstanceTypes getInstanceTypes(@RequestParam long limit);
}
