package org.folio.service.loader;

import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecordLoaderServiceImpl implements RecordLoaderService {
  @Override
  public MarcLoadResult loadSrsMarcRecords(List<String> instanceIds) {
    return new MarcLoadResult();
  }

  @Override
  public List<JsonObject> loadInventoryInstances(List<String> instanceIds) {
    return new ArrayList<>();
  }
}
