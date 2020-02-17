package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MappingServiceImpl implements MappingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MappingServiceImpl.class);

  @Override
  public List<String> map(List<JsonObject> instances) {
    LOGGER.info("Received [" + instances.size() + "] on mapping");
    return new ArrayList<>();
  }
}
