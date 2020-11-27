package org.folio.service.manager.export.strategy;

import io.vertx.core.Promise;
import org.folio.service.manager.export.ExportPayload;

/**
 * Export strategy interface, contain logic for export depends on record type
 */
public interface ExportStrategy {

  /**
   * Starts the export process for the chosen implementation: Instance, holdings or items
   *
   * @param exportPayload   {@ExportPayload}
   * @param blockingPromise blocking promise
   */
  void export(ExportPayload exportPayload, Promise<Object> blockingPromise);
}
