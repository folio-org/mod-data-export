package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MappingServiceImpl implements MappingService {
  @Override
  public List<String> map(List<JsonObject> instances) {
    throw new UnsupportedOperationException("Method is not implemented yet");
  }
}
