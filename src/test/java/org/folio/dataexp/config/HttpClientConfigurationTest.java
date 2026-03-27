package org.folio.dataexp.config;

import org.folio.dataexp.client.AlternativeTitleTypesClient;
import org.folio.dataexp.client.AuthorityClient;
import org.folio.dataexp.client.CallNumberTypesClient;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.client.ConsortiumClient;
import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.client.ContributorNameTypesClient;
import org.folio.dataexp.client.ElectronicAccessRelationshipsClient;
import org.folio.dataexp.client.EurekaUserPermissionsClient;
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
import org.folio.dataexp.client.OkapiClient;
import org.folio.dataexp.client.OkapiUserPermissionsClient;
import org.folio.dataexp.client.QueryClient;
import org.folio.dataexp.client.SearchClient;
import org.folio.dataexp.client.SearchConsortiumHoldings;
import org.folio.dataexp.client.SettingsBaseUrlClient;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.client.UserClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.folio.dataexp.config.HttpClientConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;
import java.net.URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpClientConfigurationTest {

  @Mock
  private HttpServiceProxyFactory factory;

  @Mock
  private RestClient.Builder restClientBuilder;

  @InjectMocks
  private HttpClientConfiguration httpClientConfiguration;

    @Test
  void interceptorShouldInvokeExecutionExecute() throws IOException {
    // TestMate-ba5177efb90430bc8665c8245ef70626
    // Given
    var uri = URI.create("http://localhost/test");
    var body = "test body".getBytes();
    var requestInterceptorCaptor = ArgumentCaptor.forClass(ClientHttpRequestInterceptor.class);
    var restClient = org.mockito.Mockito.mock(RestClient.class);
    var httpRequest = org.mockito.Mockito.mock(HttpRequest.class);
    var execution = org.mockito.Mockito.mock(ClientHttpRequestExecution.class);
    var httpResponse = org.mockito.Mockito.mock(ClientHttpResponse.class);
    var httpHeaders = org.mockito.Mockito.mock(HttpHeaders.class);
    when(restClientBuilder.requestInterceptor(requestInterceptorCaptor.capture())).thenReturn(restClientBuilder);
    when(restClientBuilder.build()).thenReturn(restClient);
    when(httpRequest.getURI()).thenReturn(uri);
    when(httpRequest.getHeaders()).thenReturn(httpHeaders);
    when(execution.execute(httpRequest, body)).thenReturn(httpResponse);
    // When
    httpClientConfiguration.factory(restClientBuilder);
    var capturedInterceptor = requestInterceptorCaptor.getValue();
    var actualResponse = capturedInterceptor.intercept(httpRequest, body, execution);
    // Then
    assertThat(actualResponse).isSameAs(httpResponse);
    verify(httpHeaders).add(HttpHeaders.ACCEPT_ENCODING, "identity");
    verify(execution).execute(httpRequest, body);
  }

}
