package org.folio.service.inputdatamanager;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.IOException;

@ProxyGen
public interface InputDataManager {  //NOSONAR
  String QUEUE_NAME = "input-data-manager.queue";     //NOSONAR

  static InputDataManager create(Vertx vertx) {
    return new InputDataManagerImpl(vertx);
  }

  static InputDataManager createProxy(Vertx vertx) {
    return new InputDataManagerVertxEBProxy(vertx, QUEUE_NAME);
  }

  /**
   * Perform the necessary steps to obtain a stream from the file storage that will be used to read uuids for export.
   *
   * @param request   HTTP request
   * @param params    HTTP request parameters
   */
  void start(JsonObject request, JsonObject params) throws IOException;

  /**
   * Publish the next chunk of uuilds to be exported.
   *
   * @param request   HTTP request
   * @param params    HTTP request parameters
   */
  void proceed(JsonObject request, JsonObject params);
}
