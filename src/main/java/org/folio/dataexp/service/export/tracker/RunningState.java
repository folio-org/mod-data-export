package org.folio.dataexp.service.export.tracker;

import org.springframework.stereotype.Component;

@Component
public class RunningState implements ExportState, SliceState {
  @Override
  public void trackExport(ExportContext exportContext) {
    exportContext.setExportState(this);
  }

  @Override
  public void trackSlice(ExportContext exportContext) {
    exportContext.setSliceState(this);
  }

  @Override
  public boolean isCompleted(ExportContext exportContext) {
    return false;
  }
}
