package org.folio.service.manager.exportmanager;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface ExportManager {  //NOSONAR
  String QUEUE_NAME = "export-manager.queue";     //NOSONAR

  static ExportManager create(Context context) {
    return new ExportManagerImpl(context);
  }

  static ExportManager createProxy(Vertx vertx) {
    return new ExportManagerVertxEBProxy(vertx, QUEUE_NAME);
  }

  /**
   * Runs the data-export process
   *
   * @param request    json request
   */
  void exportData(JsonObject request);
}
