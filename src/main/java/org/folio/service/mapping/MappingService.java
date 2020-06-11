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

  /**
   * This method specifically returns additional records mapped to variable Field format that can be
   * later appended to SRS records.
   *
   * @param record
   * @param mappingProfile
   * @param jobExecutionId
   * @param connectionParams
   * @return Variable Field
   */
  List<VariableField> mapFields(JsonObject record, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams);

}
