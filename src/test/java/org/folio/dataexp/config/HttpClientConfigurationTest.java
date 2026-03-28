package org.folio.dataexp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import org.folio.dataexp.TestMate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@ExtendWith(MockitoExtension.class)
class HttpClientConfigurationTest {

  @Mock private HttpServiceProxyFactory factory;

  @Mock private RestClient.Builder restClientBuilder;

  @InjectMocks private HttpClientConfiguration httpClientConfiguration;

  @Test
  @TestMate(name = "TestMate-ba5177efb90430bc8665c8245ef70626")
  void interceptorShouldInvokeExecutionExecute() throws IOException {
    // Given
    var uri = URI.create("http://localhost/test");
    var body = "test body".getBytes();
    var requestInterceptorCaptor = ArgumentCaptor.forClass(ClientHttpRequestInterceptor.class);
    var restClient = org.mockito.Mockito.mock(RestClient.class);
    var httpRequest = org.mockito.Mockito.mock(HttpRequest.class);
    var execution = org.mockito.Mockito.mock(ClientHttpRequestExecution.class);
    var httpResponse = org.mockito.Mockito.mock(ClientHttpResponse.class);
    var httpHeaders = org.mockito.Mockito.mock(HttpHeaders.class);
    when(restClientBuilder.requestInterceptor(requestInterceptorCaptor.capture()))
        .thenReturn(restClientBuilder);
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
