package org.folio.dataexp.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.OkapiClient;
import org.folio.dataexp.domain.dto.RoutingEntry;
import org.folio.dataexp.domain.dto.TimerDescriptor;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.util.Collections;

@TestPropertySource(properties = "application.clean-up-files-delay=10")
class TimerServiceTest extends BaseDataExportInitializer {
  @Autowired
  private TimerService timerService;
  @MockBean
  private OkapiClient okapiClient;

  @Test
  void shouldUpdateTimerIfDelayWasSet() {
    when(okapiClient.getTimerDescriptors(any(URI.class), anyString())).thenReturn(Collections.singletonList(new TimerDescriptor()
      .id("mod-data-export_0").routingEntry(new RoutingEntry().pathPattern("/data-export/clean-up-files").delay("15"))));

    var expectedDescriptor = new TimerDescriptor()
      .id("mod-data-export_0").routingEntry(new RoutingEntry().unit("hour").delay("10"));

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      timerService.updateCleanUpFilesTimerIfRequired();
      verify(okapiClient).updateTimer(any(URI.class), anyString(), eq(expectedDescriptor));
    }
  }

  @Test
  void shouldNotUpdateTimerIfValuesAreEqual() {
    when(okapiClient.getTimerDescriptors(any(URI.class), anyString())).thenReturn(Collections.singletonList(new TimerDescriptor()
      .id("mod-data-export_0").routingEntry(new RoutingEntry().pathPattern("/data-export/clean-up-files").delay("10"))));

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      timerService.updateCleanUpFilesTimerIfRequired();
      verify(okapiClient, times(0)).updateTimer(any(URI.class), anyString(), any(TimerDescriptor.class));
    }
  }
}
