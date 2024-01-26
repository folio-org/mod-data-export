package org.folio.dataexp.service.export;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class ExportContext {

  private boolean lastSlice;

  private boolean lastExport;

  public boolean isExportCompleted() {
    return lastSlice && lastExport;
  }

  public void reset() {
    lastExport = false;
    lastSlice = false;
  }
}
