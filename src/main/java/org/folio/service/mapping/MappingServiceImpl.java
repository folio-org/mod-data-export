package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@Service
public class MappingServiceImpl implements MappingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public List<String> map(List<JsonObject> instances) {
    LOGGER.info("Received [{}] instances on mapping", instances);
    return new ArrayList<>();
  }
}
