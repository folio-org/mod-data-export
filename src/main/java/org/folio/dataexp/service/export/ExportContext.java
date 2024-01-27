package org.folio.dataexp.service.export;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ExportContext {

  private final AtomicBoolean lastSlice;
  private final AtomicBoolean lastExport;

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
