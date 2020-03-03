package org.folio.service.manager.inputdatamanager;

import io.vertx.core.Context;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.service.manager.status.ExportStatus;

@ProxyGen
public interface InputDataManager {  //NOSONAR
  String INPUT_DATA_MANAGER_ADDRESS = "input-data-manager.queue";     //NOSONAR

  static InputDataManager create(Context context) {
    return new InputDataManagerImpl(context);
  }

  static InputDataManager createProxy(Vertx vertx) {
    return new InputDataManagerVertxEBProxy(vertx, INPUT_DATA_MANAGER_ADDRESS);
  }

  /**
   * Obtain a stream from the file storage that will be used to read uuids for export.
   *
   * @param exportRequest - entity that contains request parameters, such as
   *                      filedefinition, jobProfile, etc.
   * @param params   - okapi headers and connection parameters
   */
  void init(JsonObject exportRequest, JsonObject params);

  /**
   * Publish the next chunk of uuids to be exported. If there is no more uuids to read,
   * the export is considered completed.
   *
   * @param exportRequest - entity that contains request parameters, such as
   *                      filedefinition, jobProfile, etc.
   */
  void proceed(JsonObject exportRequest, ExportStatus status);
}
