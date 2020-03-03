package org.folio.service.manager.exportmanager;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.ExportService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.mapping.MappingService;
import org.folio.util.OkapiConnectionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

@RunWith(MockitoJUnitRunner.class)
public class ExportManagerUnitTest {

  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private ExportService exportService;
  @Mock
  private MappingService mappingService;
  @InjectMocks
  private ExportManagerImpl exportManager = new ExportManagerImpl();

  @Test
  public void exportBlocking_shouldPassExportFor_1000_Records() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Arrays.asList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams();
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId");
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(67)).loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(5)).loadInventoryInstancesBlocking(anyList(), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(2)).export(anyList(), any(FileDefinition.class));
    Mockito.verify(mappingService, Mockito.times(1)).map(anyList());
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class));
  }
}
