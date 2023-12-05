package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.InstanceFormats;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "instance-formats")
public interface InstanceFormatsClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  InstanceFormats getInstanceFormats(@RequestParam long limit);
}
