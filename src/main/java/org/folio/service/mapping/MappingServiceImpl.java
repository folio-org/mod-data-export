package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MappingServiceImpl implements MappingService {
  @Override
  public List<String> map(List<JsonObject> instances) {
    return new ArrayList<>();
  }
}
