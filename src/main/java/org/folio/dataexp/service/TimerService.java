package org.folio.dataexp.service;

import static org.folio.dataexp.util.Constants.OKAPI_URL;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.OkapiClient;
import org.folio.dataexp.domain.dto.RoutingEntry;
import org.folio.dataexp.domain.dto.TimerDescriptor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Log4j2
public class TimerService {
  private static final String CLEAN_UP_FILES_ENDPOINT = "/data-export/clean-up-files";

  private final OkapiClient okapiClient;
  private final FolioExecutionContext folioExecutionContext;

  @Value("${application.clean-up-files-interval}")
  private String cleanUpFilesInterval;

  public void updateCleanUpFilesTimerIfRequired() {
    var defaultTimer = okapiClient.getTimerDescriptors(URI.create(OKAPI_URL), folioExecutionContext.getTenantId()).stream()
      .filter(timerDescriptor -> timerDescriptor.getId().startsWith("mod-data-export_"))
      .filter(timerDescriptor -> CLEAN_UP_FILES_ENDPOINT.equals(timerDescriptor.getRoutingEntry().getPathPattern()))
      .findFirst();
    defaultTimer.ifPresent(timerDescriptor -> {
      var currentInterval = timerDescriptor.getRoutingEntry().getDelay();
      if (!cleanUpFilesInterval.equals(currentInterval)) {
        log.info("Updating clean-up files timer: existing value={}, new value={}", currentInterval, cleanUpFilesInterval);
        okapiClient.updateTimer(URI.create(OKAPI_URL), folioExecutionContext.getTenantId(), new TimerDescriptor()
          .id(timerDescriptor.getId())
          .routingEntry(new RoutingEntry().unit("hour").delay(cleanUpFilesInterval)));
      }
    });
  }
}
