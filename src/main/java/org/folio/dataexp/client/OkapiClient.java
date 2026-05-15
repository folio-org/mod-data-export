package org.folio.dataexp.client;

import static org.folio.dataexp.util.Constants.OKAPI;

import java.net.URI;
import java.util.List;
import org.folio.dataexp.domain.dto.TimerDescriptor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;

/** Feign client for interacting with Okapi proxy endpoints for timers. */
@HttpExchange(url = OKAPI, accept = MediaType.APPLICATION_JSON_VALUE)
public interface OkapiClient {
  /**
   * Retrieves timer descriptors for a given tenant.
   *
   * @param uri the Okapi URI
   * @param tenantId the tenant ID
   * @return a list of timer descriptors
   */
  @GetExchange(
      value = "/proxy/tenants/{tenantId}/timers",
      accept = MediaType.APPLICATION_JSON_VALUE)
  List<TimerDescriptor> getTimerDescriptors(URI uri, @PathVariable("tenantId") String tenantId);

  /**
   * Updates a timer descriptor for a given tenant.
   *
   * @param uri the Okapi URI
   * @param tenantId the tenant ID
   * @param timerDescriptor the timer descriptor to update
   */
  @PatchExchange(value = "/proxy/tenants/{tenantId}/timers")
  void updateTimer(
      URI uri,
      @PathVariable("tenantId") String tenantId,
      @RequestBody TimerDescriptor timerDescriptor);
}
