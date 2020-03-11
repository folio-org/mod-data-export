package org.folio.service.export.storage;

import org.springframework.stereotype.Component;

@Component
public class ExportStorageFactory {

  public ExportStorageService getExportStorage() {
    return new AWSStorageServiceImpl();
  }
}
