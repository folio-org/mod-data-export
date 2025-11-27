package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.InstanceFormats;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving instance formats. */
@FeignClient(name = "instance-formats")
public interface InstanceFormatsClient {
  /**
   * Retrieves instance formats with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of instance formats
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  InstanceFormats getInstanceFormats(@RequestParam long limit);
}
