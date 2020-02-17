package org.folio.service.loader;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecordLoaderServiceImpl implements RecordLoaderService {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordLoaderServiceImpl.class);

  @Override
  public MarcLoadResult loadSrsMarcRecords(List<String> instanceIds) {
    LOGGER.info("Received instances ids [" + instanceIds.size() + "] on SRS MARC loading");
    return new MarcLoadResult();
  }

  @Override
  public List<JsonObject> loadInventoryInstances(List<String> instanceIds) {
    LOGGER.info("Received instances ids [" + instanceIds.size() + "] on Inventory Instance loading");
    return new ArrayList<>();
  }
}
