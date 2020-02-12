package org.folio.service.fileexport;


import io.vertx.core.json.JsonObject;

import java.util.List;


/**
 * File export service
 */
public interface FileExportService {

  /**
   * Saves collection of marc records to the destination
   *
   * @param marcRecords collection of marc records
   * @return Future
   */
  void save(List<String> marcRecords);
}
