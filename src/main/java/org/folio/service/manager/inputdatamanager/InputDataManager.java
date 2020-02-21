package org.folio.service.manager.inputdatamanager;

import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.util.OkapiConnectionParams;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface InputDataManager {  //NOSONAR
  String QUEUE_NAME = "input-data-manager.queue";     //NOSONAR

  static InputDataManager create(Vertx vertx) {
    return new InputDataManagerImpl(vertx.getOrCreateContext());
  }

  static InputDataManager createProxy(Vertx vertx) {
    return new InputDataManagerVertxEBProxy(vertx, QUEUE_NAME);
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
   * @param params   - okapi headers and connection parameters
   */
  void proceed(JsonObject exportRequest, JsonObject params);
}
