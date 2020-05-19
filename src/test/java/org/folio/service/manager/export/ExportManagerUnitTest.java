package org.folio.service.manager.export;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.ExportService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.mapping.MappingService;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class ExportManagerUnitTest {
  private static final int LIMIT = 20;

  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private ExportService exportService;
  @Mock
  private MappingService mappingService;
  @InjectMocks
  private ExportManagerImpl exportManager = new ExportManagerImpl();

  @Test
  void exportBlocking_shouldPassExportFor_1000_Records() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Arrays.asList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(marcLoadResult);
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId");
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(50)).loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(recordLoaderService, Mockito.times(3)).loadInventoryInstancesBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(exportService, Mockito.times(1)).exportSrsRecord(anyList(), any(FileDefinition.class));
    Mockito.verify(mappingService, Mockito.times(1)).map(anyList(), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
  }
}
