package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import java.util.List;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.util.OkapiConnectionParams;
import org.marc4j.marc.VariableField;

public interface MappingService {

  /**
   * Performs mapping to marc records
   *
   * @param records FOLIO records
   * @param jobExecutionId job id
   * @param connectionParams okapi connection parameters
   * @return marc records
   */
  List<String> map(List<JsonObject> records, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams);

  List<VariableField> mapFields(JsonObject record, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams);

}
