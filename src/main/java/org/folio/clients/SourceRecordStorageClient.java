package org.folio.clients;

import static org.folio.util.ExternalPathResolver.SRS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Optional;
import org.folio.util.OkapiConnectionParams;
import org.springframework.stereotype.Component;

@Component
public class SourceRecordStorageClient {
  private static final String QUERY_PATTERN_SRS = "externalIdsHolder.instanceId==%s";
  private static final String QUERY_LIMIT_PATTERN = "?query=(%s)&limit=";

  public Optional<JsonObject> getRecordsByIds(List<String> ids, OkapiConnectionParams params, int partitionSize) {
    return ClientUtil.getByIds(ids, params, resourcesPathWithPrefix(SRS) + QUERY_LIMIT_PATTERN + partitionSize, QUERY_PATTERN_SRS);
  }
}
