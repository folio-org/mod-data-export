package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.OKAPI_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.client.OkapiClient;
import org.folio.dataexp.domain.dto.RoutingEntry;
import org.folio.dataexp.domain.dto.TimerDescriptor;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TimerServiceTest {

  @Mock private OkapiClient okapiClient;
  @Mock private FolioExecutionContext folioExecutionContext;
  @InjectMocks private TimerService timerService;

  @Captor private ArgumentCaptor<TimerDescriptor> timerDescriptorCaptor;

  @Test
  @TestMate(name = "TestMate-61626375f1a85b73420f5aca208e4734")
  void updateCleanUpFilesTimerIfRequiredShouldUpdateTimerWhenDelayDiffers() {
    // Given
    var tenantId = "test-tenant";
    var cleanUpFilesEndpoint = "/data-export/clean-up-files";
    var timerId = "mod-data-export_unique-id";
    var newDelay = "24";
    var oldDelay = "12";
    var okapiUri = URI.create(OKAPI_URL);
    ReflectionTestUtils.setField(timerService, "cleanUpFilesDelay", newDelay);
    var routingEntry = new RoutingEntry().pathPattern(cleanUpFilesEndpoint).delay(oldDelay);
    var existingTimer = new TimerDescriptor().id(timerId).routingEntry(routingEntry);
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(okapiClient.getTimerDescriptors(okapiUri, tenantId)).thenReturn(List.of(existingTimer));
    // When
    timerService.updateCleanUpFilesTimerIfRequired();
    // Then
    verify(okapiClient).updateTimer(eq(okapiUri), eq(tenantId), timerDescriptorCaptor.capture());
    var capturedDescriptor = timerDescriptorCaptor.getValue();
    assertThat(capturedDescriptor.getId()).isEqualTo(timerId);
    assertThat(capturedDescriptor.getRoutingEntry().getUnit()).isEqualTo("hour");
    assertThat(capturedDescriptor.getRoutingEntry().getDelay()).isEqualTo(newDelay);
  }

  @Test
  @TestMate(name = "TestMate-f5c0c509afb22a20c5676051add8a1f5")
  void updateCleanUpFilesTimerIfRequiredShouldNotUpdateTimerWhenDelayIsSame() {
    // Given
    var tenantId = "test-tenant";
    var cleanUpFilesEndpoint = "/data-export/clean-up-files";
    var timerId = "mod-data-export_123";
    var delay = "24";
    var okapiUri = URI.create(OKAPI_URL);
    ReflectionTestUtils.setField(timerService, "cleanUpFilesDelay", delay);
    var routingEntry = new RoutingEntry().pathPattern(cleanUpFilesEndpoint).delay(delay);
    var existingTimer = new TimerDescriptor().id(timerId).routingEntry(routingEntry);
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(okapiClient.getTimerDescriptors(okapiUri, tenantId)).thenReturn(List.of(existingTimer));
    // When
    timerService.updateCleanUpFilesTimerIfRequired();
    // Then
    verify(okapiClient).getTimerDescriptors(okapiUri, tenantId);
    verify(okapiClient, never())
        .updateTimer(eq(okapiUri), eq(tenantId), any(TimerDescriptor.class));
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-b9e84ad08891ec2c751f81ee1e410197")
  @ValueSource(strings = {"abc", "-5", "", "1.5"})
  void updateCleanUpFilesTimerIfRequiredShouldDoNothingWhenDelayIsInvalid(String invalidDelay) {
    // Given
    ReflectionTestUtils.setField(timerService, "cleanUpFilesDelay", invalidDelay);
    // When
    timerService.updateCleanUpFilesTimerIfRequired();
    // Then
    verify(okapiClient, never()).getTimerDescriptors(any(URI.class), anyString());
    verify(okapiClient, never())
        .updateTimer(any(URI.class), anyString(), any(TimerDescriptor.class));
    verify(folioExecutionContext, never()).getTenantId();
  }

  @Test
  @TestMate(name = "TestMate-1f4505ba4351fa3fa302ce4d277b9d86")
  void updateCleanUpFilesTimerIfRequiredShouldDoNothingWhenNoMatchingTimerFound() {
    // Given
    var tenantId = "test-tenant";
    var cleanUpFilesEndpoint = "/data-export/clean-up-files";
    var okapiUri = URI.create(OKAPI_URL);
    ReflectionTestUtils.setField(timerService, "cleanUpFilesDelay", "24");
    var timerWithWrongId =
        new TimerDescriptor()
            .id("mod-inventory_1")
            .routingEntry(new RoutingEntry().pathPattern(cleanUpFilesEndpoint).delay("12"));
    var timerWithWrongPath =
        new TimerDescriptor()
            .id("mod-data-export_abc")
            .routingEntry(new RoutingEntry().pathPattern("/other/endpoint").delay("12"));
    var nonMatchingTimers = List.of(timerWithWrongId, timerWithWrongPath);
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(okapiClient.getTimerDescriptors(okapiUri, tenantId)).thenReturn(nonMatchingTimers);
    // When
    timerService.updateCleanUpFilesTimerIfRequired();
    // Then
    verify(okapiClient).getTimerDescriptors(okapiUri, tenantId);
    verify(okapiClient, never())
        .updateTimer(any(URI.class), anyString(), any(TimerDescriptor.class));
  }
}
