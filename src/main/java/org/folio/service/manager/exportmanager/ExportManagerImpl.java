package org.folio.service.manager.exportmanager;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.service.fileexport.FileExportService;
import org.folio.service.loader.MarcLoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.mapping.MappingService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;

import static io.vertx.core.Future.*;

/**
 * The ExportManager is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
@SuppressWarnings({"java:S1172", "java:S125"})
public class ExportManagerImpl implements ExportManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExportManagerImpl.class);
  private static final int POOL_SIZE = 1;
  private static final String IDENTIFIERS_KEY = "identifiers";
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  @Autowired
  private RecordLoaderService recordLoaderService;
  @Autowired
  private FileExportService fileExportService;
  @Autowired
  private MappingService mappingService;


  public ExportManagerImpl() {
  }

  public ExportManagerImpl(Context context) {
    SpringContextUtil.autowireDependencies(this, context);
    this.executor = context.owner().createSharedWorkerExecutor("export-thread-worker", POOL_SIZE);
  }

  @Override
  public void export(JsonObject jsonRequest, JsonObject jsonRequestParams) {
    if (jsonRequest.containsKey(IDENTIFIERS_KEY)) {
      List<String> identifiers = jsonRequest.getJsonArray(IDENTIFIERS_KEY).getList();
      OkapiConnectionParams requestParams = new OkapiConnectionParams(jsonRequestParams.mapTo(HashMap.class));
      this.executor.executeBlocking(blockingFuture -> exportBlocking(identifiers, requestParams), this::handleExportResult);
    } else {
      String errorMessage = "Can not find identifiers, request: " + jsonRequest;
      LOGGER.error(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   *
   * @param asyncResult
   * @return
   */
  private Future<Void> handleExportResult(AsyncResult asyncResult) {
    if (asyncResult.failed()) {
      LOGGER.error("Export is failed, cause: " + asyncResult.cause());
    } else {
      LOGGER.info("Export has been successfully passed");
      // TODO call proxy1 to request more UUIDs
    }
    return succeededFuture();
  }

  /**
   *  Runs the main export flow in blocking manner.
   *
   */
  protected void exportBlocking(List<String> identifiers, OkapiConnectionParams params) {
    MarcLoadResult marcLoadResult = recordLoaderService.loadMarcByInstanceIds(identifiers, params);
    List<String> srsMarcRecords = marcLoadResult.getSrsMarcRecords();
    fileExportService.save(srsMarcRecords);
    List<JsonObject> instances = recordLoaderService.loadInstancesByIds(marcLoadResult.getInstanceIds());
    List<String> mappedMarcRecords = mappingService.map(instances);
    fileExportService.save(mappedMarcRecords);
  }
}
