package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;

import java.util.List;

public interface MappingService {

  /**
   * Performs mapping to marc records
   *
   * @param records FOLIO records
   * @return lit of strings
   */
  List<String> map(List<JsonObject> records);
}
