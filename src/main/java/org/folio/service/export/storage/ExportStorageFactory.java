package org.folio.service.export.storage;

import org.springframework.stereotype.Component;

@Component
public class ExportStorageFactory {
  private final String AWS_KEY = "AWSS3";

  public ExportStorageService getExportStorage() {
    return new AWSStorageServiceImpl();
  }
}
