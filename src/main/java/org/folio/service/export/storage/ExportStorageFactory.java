package org.folio.service.export.storage;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExportStorageFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  public ExportStorageService getExportStorageImplementation(String type) {
    ExportStorageService ret;

    if (type == null)
      type = "";

    //add additional implementations later
    switch (type) {
      case "AWSS3":
        ret = new AWSStorageServiceImpl();
        break;
      default:
        ret = new AWSStorageServiceImpl();
    }


    LOGGER.info(String.format("type: %s, class: %s", type, ret.getClass()
      .getName()));
    return ret;
  }

}
