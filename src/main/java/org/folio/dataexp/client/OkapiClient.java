package org.folio.dataexp.client;

import java.net.URI;
import java.util.List;
import org.folio.dataexp.domain.dto.TimerDescriptor;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for interacting with Okapi proxy endpoints for timers.
 */
@FeignClient(name = "okapi", configuration = FeignClientConfiguration.class)
public interface OkapiClient {
  /**
   * Retrieves timer descriptors for a given tenant.
   *
   * @param uri the Okapi URI
   * @param tenantId the tenant ID
   * @return a list of timer descriptors
   */
  @GetMapping(value = "/proxy/tenants/{tenantId}/timers",
      produces = MediaType.APPLICATION_JSON_VALUE)
  List<TimerDescriptor> getTimerDescriptors(URI uri, @PathVariable("tenantId") String tenantId);

  /**
   * Updates a timer descriptor for a given tenant.
   *
   * @param uri the Okapi URI
   * @param tenantId the tenant ID
   * @param timerDescriptor the timer descriptor to update
   */
  @PatchMapping(value = "/proxy/tenants/{tenantId}/timers")
  void updateTimer(URI uri, @PathVariable("tenantId") String tenantId,
      @RequestBody TimerDescriptor timerDescriptor);
}
