package org.folio.dataexp.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
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

  @Value("${application.clean-up-files-delay}")
  private String cleanUpFilesDelay;

  public void updateCleanUpFilesTimerIfRequired() {
    var newValue = validateValueForTimer(cleanUpFilesDelay);
    if (isNotEmpty(newValue)) {
      var existingTimer = okapiClient.getTimerDescriptors(URI.create(OKAPI_URL), folioExecutionContext.getTenantId()).stream()
        .filter(timerDescriptor -> timerDescriptor.getId().startsWith("mod-data-export_"))
        .filter(timerDescriptor -> CLEAN_UP_FILES_ENDPOINT.equals(timerDescriptor.getRoutingEntry().getPathPattern()))
        .findFirst();
      existingTimer.ifPresent(timerDescriptor -> {
        var currentValue = timerDescriptor.getRoutingEntry().getDelay();
        if (!cleanUpFilesDelay.equals(currentValue)) {
          log.info("Updating clean-up files timer delay: existing value={}, new value={}", currentValue, cleanUpFilesDelay);
          okapiClient.updateTimer(URI.create(OKAPI_URL), folioExecutionContext.getTenantId(), new TimerDescriptor()
            .id(timerDescriptor.getId())
            .routingEntry(new RoutingEntry().unit("hour").delay(cleanUpFilesDelay)));
        }
      });
    }
  }

  private String validateValueForTimer(String value) {
    if (isNotEmpty(value)) {
      try {
        Integer.parseUnsignedInt(value);
      } catch (NumberFormatException e) {
        log.error("Invalid value for clean-up files timer delay: {}", value);
        return EMPTY;
      }
    }
    return value;
  }
}
