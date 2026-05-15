package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.InstanceFormats;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving instance formats. */
@HttpExchange(url = "instance-formats")
public interface InstanceFormatsClient {
  /**
   * Retrieves instance formats with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of instance formats
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  InstanceFormats getInstanceFormats(@RequestParam long limit);
}
