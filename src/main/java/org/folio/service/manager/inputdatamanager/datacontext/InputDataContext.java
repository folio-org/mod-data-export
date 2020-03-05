package org.folio.service.manager.inputdatamanager.datacontext;

import io.vertx.core.shareddata.Shareable;

import java.util.Iterator;
import java.util.List;

public class InputDataContext implements Shareable {

  private Iterator<List<String>> fileContentIterator;

  public InputDataContext(Iterator<List<String>> sourceStream) {
    this.fileContentIterator = sourceStream;
  }

  public Iterator<List<String>> getFileContentIterator() {
    return fileContentIterator;
  }

  public void setFileContentIterator(Iterator<List<String>> fileContentIterator) {
    this.fileContentIterator = fileContentIterator;
  }
}
