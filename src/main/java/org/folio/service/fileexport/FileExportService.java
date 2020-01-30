package org.folio.service.fileexport;

import io.vertx.core.Future;

import java.util.List;

import static io.vertx.core.Future.succeededFuture;

/**
 * File export service
 */
public interface FileExportService {

  /**
   * Saves collection of marc records to the destination
   *
   * @param destination a place where to save records
   * @param marcRecords collection of marc records
   * @return Future
   */
   Future<Void> save(String destination, List<String> marcRecords);
}
