package org.folio.service.export.storage;

import org.springframework.stereotype.Component;

@Component
public class ExportStorageFactory {

  /**
   * Currently supports only Amazon S3, more implementations will be added
   * @return
   */
  public ExportStorageService getExportStorageImplementation() {
    return new AWSStorageServiceImpl();
  }

}
