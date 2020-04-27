package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;

import java.util.List;

public interface MappingService {

  /**
   * Performs mapping to marc records
   *
   * @param records FOLIO records
   * @return marc records
   */
  List<String> map(List<JsonObject> records, String jobExecutionId, OkapiConnectionParams connectionParams);
}
