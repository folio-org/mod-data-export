package org.folio.service.manager.inputdatamanager;

import io.vertx.core.shareddata.Shareable;
import org.folio.service.manager.inputdatamanager.reader.SourceReader;

public class InputDataContext implements Shareable {

  private SourceReader sourceReader;

  public InputDataContext(SourceReader sourceReader) {
    this.sourceReader = sourceReader;
  }

  public SourceReader getSourceReader() {
    return sourceReader;
  }

  public void setSourceReader(SourceReader sourceReader) {
    this.sourceReader = sourceReader;
  }
}
