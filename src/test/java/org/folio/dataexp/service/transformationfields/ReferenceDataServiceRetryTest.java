package org.folio.dataexp.service.transformationfields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.folio.dataexp.client.AlternativeTitleTypesClient;
import org.folio.dataexp.client.CallNumberTypesClient;
import org.folio.dataexp.client.ContributorNameTypesClient;
import org.folio.dataexp.client.ElectronicAccessRelationshipsClient;
import org.folio.dataexp.client.HoldingsNoteTypesClient;
import org.folio.dataexp.client.IdentifierTypesClient;
import org.folio.dataexp.client.InstanceFormatsClient;
import org.folio.dataexp.client.InstanceTypesClient;
import org.folio.dataexp.client.IssuanceModesClient;
import org.folio.dataexp.client.ItemNoteTypesClient;
import org.folio.dataexp.client.LoanTypesClient;
import org.folio.dataexp.client.LocationUnitsClient;
import org.folio.dataexp.client.LocationsClient;
import org.folio.dataexp.client.MaterialTypesClient;
import org.folio.dataexp.client.NatureOfContentTermsClient;
import org.folio.dataexp.domain.dto.AlternativeDataTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {ReferenceDataService.class, ReferenceDataServiceRetryTest.RetryConfig.class})
@TestPropertySource(
    properties = {
      "application.feign-client-retry.max-attempts=3",
      "application.feign-client-retry.initial-wait-time=0",
      "application.feign-client-retry.max-wait-time=0"
    })
class ReferenceDataServiceRetryTest {

  @Configuration
  @EnableRetry
  static class RetryConfig {}

  @MockitoBean ObjectMapper objectMapper;
  @MockitoBean AlternativeTitleTypesClient alternativeTitleTypesClient;
  @MockitoBean CallNumberTypesClient callNumberTypesClient;
  @MockitoBean ContributorNameTypesClient contributorNameTypesClient;
  @MockitoBean ElectronicAccessRelationshipsClient electronicAccessRelationshipsClient;
  @MockitoBean HoldingsNoteTypesClient holdingsNoteTypesClient;
  @MockitoBean IdentifierTypesClient identifierTypesClient;
  @MockitoBean InstanceFormatsClient instanceFormatsClient;
  @MockitoBean InstanceTypesClient instanceTypesClient;
  @MockitoBean IssuanceModesClient issuanceModesClient;
  @MockitoBean ItemNoteTypesClient itemNoteTypesClient;
  @MockitoBean LoanTypesClient loanTypesClient;
  @MockitoBean LocationUnitsClient locationUnitsClient;
  @MockitoBean LocationsClient locationsClient;
  @MockitoBean MaterialTypesClient materialTypesClient;
  @MockitoBean NatureOfContentTermsClient natureOfContentTermsClient;

  @Autowired private ReferenceDataService referenceDataService;

  @Test
  void getAlternativeTitleTypesShouldRetryOnTransientFailure() {
    var emptyResponse = new AlternativeDataTypes().alternativeTitleTypes(Collections.emptyList());
    when(alternativeTitleTypesClient.getAlternativeTitleTypes(anyLong()))
        .thenThrow(new RuntimeException("transient"))
        .thenReturn(emptyResponse);

    var result = referenceDataService.getAlternativeTitleTypes();

    assertThat(result).isEmpty();
    verify(alternativeTitleTypesClient, times(2)).getAlternativeTitleTypes(anyLong());
  }

  @Test
  void getAlternativeTitleTypesShouldThrowAfterMaxAttempts() {
    when(alternativeTitleTypesClient.getAlternativeTitleTypes(anyLong()))
        .thenThrow(new RuntimeException("persistent"));

    assertThrows(RuntimeException.class, () -> referenceDataService.getAlternativeTitleTypes());
    verify(alternativeTitleTypesClient, times(3)).getAlternativeTitleTypes(anyLong());
  }
}
