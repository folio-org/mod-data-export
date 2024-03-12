package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.TimerDescriptor;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.List;

@FeignClient(name = "okapi", configuration = FeignClientConfiguration.class)
public interface OkapiClient {
  @GetMapping(value = "/proxy/tenants/{tenantId}/timers", produces = MediaType.APPLICATION_JSON_VALUE)
  List<TimerDescriptor> getTimerDescriptors(URI uri, @PathVariable("tenantId") String tenantId);

  @PatchMapping(value = "/proxy/tenants/{tenantId}/timers")
  void updateTimer(URI uri, @PathVariable("tenantId") String tenantId, @RequestBody TimerDescriptor timerDescriptor);
}
