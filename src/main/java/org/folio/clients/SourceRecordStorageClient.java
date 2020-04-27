package org.folio.clients;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SourceRecordStorageClient {
  private static final String GET_RECORDS_PATTERN_SRS = "%s/source-storage/records?query=(%s)";
  private static final String QUERY_PATTERN_SRS = "externalIdsHolder.instanceId==%s";
  private static final String LIMIT_PATTERN = "&limit=";

  public Optional<JsonObject> getRecordsByIds(List<String> ids, OkapiConnectionParams params, int partitionSize) {
    return ClientUtil.getByIds(ids, params, GET_RECORDS_PATTERN_SRS + LIMIT_PATTERN + partitionSize, QUERY_PATTERN_SRS);
  }
}
