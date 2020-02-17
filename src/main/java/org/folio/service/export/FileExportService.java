package org.folio.service.export;

import java.util.List;

/**
 * File export service
 */
public interface FileExportService {

  /**
   * Export collection of marc records to the destination
   *
   * @param marcRecords collection of marc records on export
   */
  void export(List<String> marcRecords);
}
