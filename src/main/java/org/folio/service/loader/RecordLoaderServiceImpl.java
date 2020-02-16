package org.folio.service.loader;

import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecordLoaderServiceImpl implements RecordLoaderService {
  @Override
  public MarcLoadResult loadMarcByInstanceIds(List<String> uuids) {
    throw new UnsupportedOperationException("Method is not implemented yet");
  }

  @Override
  public List<JsonObject> loadInstancesByIds(List<String> instanceIds) {
    throw new UnsupportedOperationException("Method is not implemented yet");
  }
}
