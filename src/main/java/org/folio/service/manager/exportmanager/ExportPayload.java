package org.folio.service.manager.exportmanager;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.util.OkapiConnectionParams;

import java.util.List;

public class ExportPayload {
  private List<String> identifiers;
  private boolean last;
  private FileDefinition fileExportDefinition;
  private OkapiConnectionParams okapiConnectionParams;

  public ExportPayload(List<String> identifiers, boolean last, FileDefinition fileExportDefinition, OkapiConnectionParams okapiConnectionParams) {
    this.identifiers = identifiers;
    this.last = last;
    this.fileExportDefinition = fileExportDefinition;
    this.okapiConnectionParams = okapiConnectionParams;
  }

  public List<String> getIdentifiers() {
    return identifiers;
  }

  public boolean isLast() {
    return last;
  }

  public FileDefinition getFileExportDefinition() {
    return fileExportDefinition;
  }

  public OkapiConnectionParams getOkapiConnectionParams() {
    return okapiConnectionParams;
  }
}
