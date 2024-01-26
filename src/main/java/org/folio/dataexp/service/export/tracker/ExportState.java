package org.folio.dataexp.service.export.tracker;

public interface ExportState {

  void trackExport(ExportContext exportContext);
  void trackSlice(ExportContext exportContext);
  boolean isCompleted(ExportContext exportContext);
}
