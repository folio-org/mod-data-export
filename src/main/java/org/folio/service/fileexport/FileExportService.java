package org.folio.service.fileexport;

import io.vertx.core.Future;

import java.util.List;

import static io.vertx.core.Future.succeededFuture;

public interface FileExportService {

  default Future<Void> save(String fileId, List<String> marcRecords) {
    return succeededFuture();
  }
}
