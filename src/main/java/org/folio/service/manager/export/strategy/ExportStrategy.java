package org.folio.service.manager.export.strategy;

import io.vertx.core.Promise;
import org.folio.service.manager.export.ExportPayload;

public interface ExportStrategy {

  void export(ExportPayload exportPayload, Promise<Object> blockingPromise);
}
