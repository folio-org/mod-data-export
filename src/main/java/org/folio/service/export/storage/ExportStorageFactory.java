package org.folio.service.export.storage;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExportStorageFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private AmazonClient amazonClient;
  public ExportStorageService getExportStorageImplementation(String type) {
    ExportStorageService ret;

    if (type == null)
      type = "";

    //add additional implementations later
    switch (type) {
      case "AWSS3":
        ret = new AWSStorageServiceImpl(amazonClient);
        break;
      default:
        ret = new AWSStorageServiceImpl(amazonClient);
    }


    LOGGER.info(String.format("type: %s, class: %s", type, ret.getClass()
      .getName()));
    return ret;
  }

}
