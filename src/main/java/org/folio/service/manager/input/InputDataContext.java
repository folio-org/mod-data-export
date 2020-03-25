package org.folio.service.manager.input;

import io.vertx.core.shareddata.Shareable;
import org.folio.service.file.reader.SourceReader;

public class InputDataContext implements Shareable {

  private SourceReader sourceReader;
  private int totalRecordsNumber;

  public InputDataContext(SourceReader sourceReader) {
    this.sourceReader = sourceReader;
  }

  public SourceReader getSourceReader() {
    return sourceReader;
  }

  public void setSourceReader(SourceReader sourceReader) {
    this.sourceReader = sourceReader;
  }

  public int getTotalRecordsNumber() {
    return totalRecordsNumber;
  }

  public void setTotalRecordsNumber(int totalRecordsNumber) {
    this.totalRecordsNumber = totalRecordsNumber;
  }
}
