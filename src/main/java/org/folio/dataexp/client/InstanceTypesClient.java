package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.InstanceTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving instance types. */
@HttpExchange(url = "instance-types")
public interface InstanceTypesClient {
  /**
   * Retrieves instance types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of instance types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  InstanceTypes getInstanceTypes(@RequestParam long limit);
}
