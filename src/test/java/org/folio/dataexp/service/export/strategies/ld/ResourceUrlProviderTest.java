package org.folio.dataexp.service.export.strategies.ld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.folio.dataexp.service.BaseUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceUrlProviderTest {

  @Mock private BaseUrlService baseUrlService;

  @InjectMocks private ResourceUrlProvider resourceUrlProvider;

  @Test
  void applyReturnsExpectedResourceUrl() {
    when(baseUrlService.getBaseUrl()).thenReturn("http://folio.example");

    var result = resourceUrlProvider.apply(12345L);

    assertEquals("http://folio.example/linked-data-editor/resources/12345", result);
  }
}
