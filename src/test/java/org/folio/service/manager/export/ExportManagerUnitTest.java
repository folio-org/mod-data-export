package org.folio.service.manager.export;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.export.ExportService;
import org.folio.service.loader.InventoryLoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.mapping.converter.InventoryRecordConverterService;
import org.folio.service.mapping.converter.SrsRecordConverterService;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class ExportManagerUnitTest {
  private static final int LIMIT = 50;
  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private ExportService exportService;
  @Mock
  private SrsRecordConverterService srsRecordService;
  @Mock
  private InventoryRecordConverterService inventoryRecordService;
  @Mock
  private ErrorLogService errorLogService;
  @InjectMocks
  private ExportManagerImpl exportManager = Mockito.spy(new ExportManagerImpl());

  @Test
  void exportBlocking_shouldPassExportFor_1000_Records() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    InventoryLoadResult inventoryLoadResult = Mockito.mock(InventoryLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(inventoryLoadResult.getNotFoundInstancesUUIDs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), anyString(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyCollection(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(inventoryLoadResult);
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", new MappingProfile());
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(20)).loadMarcRecordsBlocking(anyList(), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(1)).loadInventoryInstancesBlocking(anyList(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(exportService, Mockito.times(1)).exportSrsRecord(anyList(), any(FileDefinition.class));
    Mockito.verify(inventoryRecordService, Mockito.times(1)).transformInventoryRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
    Mockito.verify(errorLogService).saveGeneralError(anyString(), anyString(), anyString());
  }

}
