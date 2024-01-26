package org.folio.dataexp.service.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ExportContext {

  @Autowired
  private AtomicBoolean lastSlice;

  @Autowired
  private AtomicBoolean lastExport;

  public void setLastSlice(boolean lastSlice) {
    this.lastSlice.set(lastSlice);
  }

  public void setLastExport(boolean lastExport) {
    this.lastExport.set(lastExport);
  }

  public boolean isExportCompleted() {
    return lastSlice.get() && lastExport.get();
  }

  public void reset() {
    lastExport.set(false);
    lastSlice.set(false);
  }
}
