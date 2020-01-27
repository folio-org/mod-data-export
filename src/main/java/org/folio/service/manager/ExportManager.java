package org.folio.service.manager;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface ExportManager {
  String EXPORT_MANAGER_ADDRESS = "export-manager.queue";     // NOSONAR

  static ExportManager create(Vertx vertx) {
    return new ExportManagerImpl(vertx);
  }

  static ExportManager createProxy(Vertx vertx) {
    return new ExportManagerVertxEBProxy(vertx, EXPORT_MANAGER_ADDRESS);
  }

  /**
   * Starts the data-export process in background thread.
   * @param request   HTTP request
   * @param params    HTTP request parameters
   */
  void startExport(JsonObject request, JsonObject params);
}
