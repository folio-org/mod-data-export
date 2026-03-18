package org.folio.dataexp.service;

import static org.folio.dataexp.util.FolioExecutionContextUtil.prepareContextForTenant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.client.SettingsBaseUrlClient;
import org.folio.dataexp.domain.dto.BaseUrl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

class BaseUrlServiceIT extends BaseDataExportInitializerIT {

  private static final String FIRST_TENANT = "diku";
  private static final String SECOND_TENANT = "college";

  @Autowired private BaseUrlService baseUrlService;
  @Autowired private CacheManager cacheManager;
  @Autowired private FolioModuleMetadata folioModuleMetadata;
  @Autowired private FolioExecutionContext currentExecutionContext;
  @Autowired private SettingsBaseUrlClient settingsBaseUrlClient;

  @BeforeEach
  void setUp() {
    clearBaseUrlCache();
  }

  @AfterEach
  void tearDown() {
    clearBaseUrlCache();
  }

  private void clearBaseUrlCache() {
    var cache = cacheManager.getCache("baseUrl");
    if (cache != null) {
      cache.clear();
    }
  }

  @Test
  void getBaseUrlCachesValuesPerTenant() {
    stubBaseUrlClient(settingsBaseUrlClient);
    clearInvocations(settingsBaseUrlClient);

    var firstTenantContext =
        prepareContextForTenant(FIRST_TENANT, folioModuleMetadata, folioExecutionContext);
    try (var context = new FolioExecutionContextSetter(firstTenantContext)) {
      assertEquals("https://diku.example.org", baseUrlService.getBaseUrl());
      assertEquals("https://diku.example.org", baseUrlService.getBaseUrl());
    }

    var secondTenantContext =
        prepareContextForTenant(SECOND_TENANT, folioModuleMetadata, folioExecutionContext);
    try (var context = new FolioExecutionContextSetter(secondTenantContext)) {
      assertEquals("https://college.example.org", baseUrlService.getBaseUrl());
      assertEquals("https://college.example.org", baseUrlService.getBaseUrl());
    }

    try (var context = new FolioExecutionContextSetter(firstTenantContext)) {
      assertEquals("https://diku.example.org", baseUrlService.getBaseUrl());
    }
    verify(settingsBaseUrlClient, times(2)).getBaseUrl();

    var baseUrlCache = (CaffeineCache) cacheManager.getCache("baseUrl");
    assertEquals(2, baseUrlCache.getNativeCache().asMap().size());
    assertEquals("https://diku.example.org", baseUrlCache.get("diku_base-url").get());
    assertEquals("https://college.example.org", baseUrlCache.get("college_base-url").get());
  }

  private void stubBaseUrlClient(SettingsBaseUrlClient settingsBaseUrlClient) {
    doAnswer(
            x ->
                new BaseUrl()
                    .baseUrl("https://" + currentExecutionContext.getTenantId() + ".example.org"))
        .when(settingsBaseUrlClient)
        .getBaseUrl();
  }
}
