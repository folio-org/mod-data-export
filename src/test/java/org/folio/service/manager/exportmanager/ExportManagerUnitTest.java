package org.folio.service.manager.exportmanager;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.assertj.core.api.Assertions;
import org.folio.service.config.ApplicationTestConfig;
import org.folio.service.fileexport.FileExportService;
import org.folio.service.loader.MarcLoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.manager.exportmanager.ExportManagerImpl;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

@RunWith(VertxUnitRunner.class)
public class ExportManagerUnitTest {

  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private FileExportService fileExportService;
  @InjectMocks
  private ExportManagerImpl exportManagerWithMocks = new ExportManagerImpl();

  private Context vertxContext = Vertx.vertx().getOrCreateContext();

  @Test
  public void exportBlocking_shouldPassExport() {
    // given
    MarcLoadResult marcLoadResult = new MarcLoadResult(Collections.emptyList(), Collections.emptyList());
    Mockito.when(recordLoaderService.loadMarcByInstanceIds(anyList(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    // when
    exportManagerWithMocks.exportBlocking(new ArrayList<>(), new OkapiConnectionParams());
    // then assert number of method calls

  }
}
