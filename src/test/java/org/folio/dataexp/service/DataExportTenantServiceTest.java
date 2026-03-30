package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DataExportTenantServiceTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private FolioExecutionContext context;
  @Mock private FolioSpringLiquibase folioSpringLiquibase;
  @Mock private JobProfileEntityRepository jobProfileEntityRepository;
  @Mock private MappingProfileEntityRepository mappingProfileEntityRepository;
  @Mock private ConfigurationService configurationService;
  @Mock private TimerService timerService;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private DataExportTenantService dataExportTenantService;

  @Mock private FolioModuleMetadata folioModuleMetadata;

  @Test
  @TestMate(name = "TestMate-55e7ca0e41910724881d2059e6388413")
  void testCreateOrUpdateTenantShouldInitializeDefaultConfigurations() {
    // Given
    var tenantId = "test_tenant";
    var inventoryConfig =
        new Config().key("inventory_record_link").value("http://localhost/inventory/view/");
    var sliceSizeConfig =
        new Config()
            .key(SlicerProcessor.SLICE_SIZE_KEY)
            .value(String.valueOf(SlicerProcessor.DEFAULT_SLICE_SIZE));

    when(context.getTenantId()).thenReturn(tenantId);
    when(context.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    when(configurationService.produceInventoryRecordLinkBasedOnTenantBaseUrl())
        .thenReturn(inventoryConfig);
    when(configurationService.upsertConfiguration(any(Config.class)))
        .thenReturn(inventoryConfig, sliceSizeConfig);

    var tenantAttributes = new TenantAttributes();

    // When
    dataExportTenantService.createOrUpdateTenant(tenantAttributes);

    // Then
    var configCaptor = ArgumentCaptor.forClass(Config.class);
    verify(configurationService, times(2))
        .upsertConfiguration(configCaptor.capture());
    var capturedConfigs = configCaptor.getAllValues();
    assertThat(capturedConfigs)
        .extracting(Config::getKey)
        .containsExactlyInAnyOrder("inventory_record_link", SlicerProcessor.SLICE_SIZE_KEY);
    assertThat(capturedConfigs)
        .extracting(Config::getValue)
        .contains(String.valueOf(SlicerProcessor.DEFAULT_SLICE_SIZE));
    verify(timerService).updateCleanUpFilesTimerIfRequired();
    assertThat(System.getProperty(DataExportTenantService.TENANT_FOR_VIEWS)).isEqualTo(tenantId);
  }
}
