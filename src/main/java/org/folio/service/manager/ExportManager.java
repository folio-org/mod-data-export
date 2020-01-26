package org.folio.service.manager;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface ExportManager {
  String EVENT_BUS_ADDRESS = "export-manager.queue";

  static ExportManager create(Vertx vertx) {
    return new ExportManagerImpl(vertx);
  }

  static ExportManager createProxy(Vertx vertx) {
    return new ExportManagerVertxEBProxy(vertx, EVENT_BUS_ADDRESS);
  }

  void startExport(JsonObject request, JsonObject params);
}
