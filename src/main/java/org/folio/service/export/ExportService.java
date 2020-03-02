package org.folio.service.export;

import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.List;

/**
 *  Export service
 */
public interface ExportService {

  /**
   * Exports collection of marc records to the destination
   *
   * @param marcRecords collection of marc records on export
   */
  void export(List<String> marcRecords, FileDefinition fileDefinition);

  /**
   *
   * @param fileDefinition
   */
  void postExport(FileDefinition fileDefinition);
}
