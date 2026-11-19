package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dataexp.client.SettingsBaseUrlClient;
import org.folio.dataexp.domain.dto.BaseUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseUrlServiceTest {

  @Mock private SettingsBaseUrlClient settingsBaseUrlClient;

  @InjectMocks private BaseUrlService baseUrlService;

  @Test
  void getBaseUrlShouldReturnUrlFromClient() {
    var baseUrl = new BaseUrl().baseUrl("http://folio.example.com");
    when(settingsBaseUrlClient.getBaseUrl()).thenReturn(baseUrl);

    var result = baseUrlService.getBaseUrl();

    assertThat(result).isEqualTo("http://folio.example.com");
    verify(settingsBaseUrlClient).getBaseUrl();
  }
}
