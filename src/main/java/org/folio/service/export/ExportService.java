package org.folio.service.export;

import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.List;

/**
 * Export service
 */
public interface ExportService {

  /**
   * Exports collection of srs records to the destination.
   * Performs converting from json to marc format.
   *
   * @param jsonRecords    collection of srs records on export
   * @param fileDefinition definition of file on export
   */
  void exportSrsRecord(List<String> jsonRecords, FileDefinition fileDefinition);

  /**
   * Exports collection of marc records to the destination.
   *
   * @param inventoryRecords collection of marc inventory records
   * @param fileDefinition   definition of file on export
   */
  void exportInventoryRecords(List<String> inventoryRecords, FileDefinition fileDefinition);

  /**
   * Performs post export logic
   *
   * @param fileDefinition file definition
   * @param tenantId       tenant id
   */
  void postExport(FileDefinition fileDefinition, String tenantId);
}
