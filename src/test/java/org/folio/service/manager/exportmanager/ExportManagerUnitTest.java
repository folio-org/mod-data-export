package org.folio.service.manager.exportmanager;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.folio.service.ApplicationTestConfig;
import org.folio.service.export.FileExportService;
import org.folio.service.loader.MarcLoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.mapping.MappingService;
import org.folio.spring.SpringContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;

@RunWith(MockitoJUnitRunner.class)
public class ExportManagerUnitTest {

  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private FileExportService fileExportService;
  @Mock
  private MappingService mappingService;
  @InjectMocks
  private ExportManagerImpl exportManagerWithMocks = new ExportManagerImpl();

  @Test
  public void export_shouldAcceptCorrectRequest() {
    // given
    Context vertxContext = Vertx.vertx().getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationTestConfig.class);

    JsonObject request = new JsonObject().put("identifiers", Collections.emptyList());
    JsonObject okapiConnectionParams = new JsonObject();
    // when
    ExportManager exportManager = new ExportManagerImpl(vertxContext);
    // then assert that no exception thrown
    Assertions.assertThatCode(() -> exportManager.exportData(request, okapiConnectionParams))
      .doesNotThrowAnyException();
  }

  @Test(expected = IllegalArgumentException.class)
  public void export_shouldNotAcceptWrongRequest() {
    // given
    Context vertxContext = Vertx.vertx().getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationTestConfig.class);

    ExportManager exportManager = new ExportManagerImpl(vertxContext);
    JsonObject request = new JsonObject();
    JsonObject okapiConnectionParams = new JsonObject();
    // when
    exportManager.exportData(request, okapiConnectionParams);
    // then expect IllegalArgumentException
  }

  @Test
  public void exportBlocking_shouldPassExport() {
    // given
    int identifiersListSize = 1000;
    Mockito.when(recordLoaderService.loadSrsMarcRecords(anyList())).thenReturn(new MarcLoadResult());
    List<String> identifiers = Mockito.mock(List.class);
    Mockito.when(identifiers.size()).thenReturn(identifiersListSize);
    // when
    exportManagerWithMocks.exportBlocking(identifiers);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(67)).loadSrsMarcRecords(anyList());
    Mockito.verify(recordLoaderService, Mockito.times(67)).loadInventoryInstances(anyList());
    Mockito.verify(fileExportService, Mockito.times(134)).export(anyList());
    Mockito.verify(mappingService, Mockito.times(67)).map(anyList());
  }
}
