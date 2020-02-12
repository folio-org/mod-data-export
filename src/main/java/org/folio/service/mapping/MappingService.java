package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;

import java.util.List;

public interface MappingService {

  List<String> map(List<JsonObject> instances);
}
