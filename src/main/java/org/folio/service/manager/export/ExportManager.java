package org.folio.service.manager.export;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface ExportManager {  //NOSONAR
  String EXPORT_MANAGER_ADDRESS = "export-manager.queue";     //NOSONAR

  static ExportManager create(Context context) {
    return new ExportManagerImpl(context);
  }

  static ExportManager createProxy(Vertx vertx) {
    return new ExportManagerVertxEBProxy(vertx, EXPORT_MANAGER_ADDRESS);
  }

  /**
   * Runs the data-export process
   *
   * @param request    json request
   */
  void exportData(JsonObject request);
}
