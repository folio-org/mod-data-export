package org.folio.service.manager.exportmanager;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.ExportService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.mapping.MappingService;
import org.folio.service.upload.definition.FileDefinitionService;
import org.folio.util.OkapiConnectionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class ExportManagerUnitTest {

  private static final String FILE_DEFINITION_ID = "c8b50e3f-0446-429c-960e-03774b88223f";
  private static final String TENANT_ID = "diku";

  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private ExportService exportService;
  @Mock
  private MappingService mappingService;
  @Mock
  private FileDefinitionService fileDefinitionService;
  @InjectMocks
  private ExportManagerImpl exportManager = new ExportManagerImpl();

  @Test
  public void exportBlocking_shouldPassExportFor_1000_Records() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Arrays.asList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    Optional<FileDefinition> fileExportDefinitionOptional = Optional.of(new FileDefinition());
    Mockito.when(fileDefinitionService.getById(FILE_DEFINITION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(fileExportDefinitionOptional));
    boolean isLast = true;
    Map<String, String> params = new HashMap<>();
    params.put("x-okapi-tenant", TENANT_ID);
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, FILE_DEFINITION_ID, okapiConnectionParams, "jobExecutionId");
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(67)).loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(5)).loadInventoryInstancesBlocking(anyList(), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(2)).export(anyList(), any(FileDefinition.class));
    Mockito.verify(mappingService, Mockito.times(1)).map(anyList());
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
  }
}
