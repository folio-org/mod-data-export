package org.folio.service.manager.inputdatamanager.datacontext;

import io.vertx.core.shareddata.Shareable;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.Iterator;
import java.util.List;

public class InputDataContext implements Shareable {

  private Iterator<List<String>> sourceStream;

  public InputDataContext(Iterator<List<String>> sourceStream) {
    this.sourceStream = sourceStream;
  }

  public Iterator<List<String>> getSourceStream() {
    return sourceStream;
  }

  public void setSourceStream(Iterator<List<String>> sourceStream) {
    this.sourceStream = sourceStream;
  }
}
