package org.folio.dataexp.service.export.tracker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class ExportContext {

  @Getter
  @Setter
  private ExportState exportState;

  @Getter
  @Setter
  private SliceState sliceState;

}
