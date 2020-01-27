package org.folio.service.fileexport;

import io.vertx.core.Future;

import java.util.List;

import static io.vertx.core.Future.succeededFuture;

/**
 * File export service
 */
public interface FileExportService {

  /**
   * Saves collection of marc records to the file
   *
   * @param fileId      id of the file where to save records
   * @param marcRecords collection of marc records
   * @return Future
   */
  default Future<Void> save(String fileId, List<String> marcRecords) { //NOSONAR
    return succeededFuture(); //NOSONAR
  }
}
