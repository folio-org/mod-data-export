package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;

import java.util.List;

public interface MappingService {

  /**
   * Performs mapping to marc records
   *
   * @param records FOLIO records
   * @param jobExecutionId job id
   * @param connectionParams okapi connection parameters
   * @return marc records
   */
  List<String> map(List<JsonObject> records, String jobExecutionId, OkapiConnectionParams connectionParams);
}
