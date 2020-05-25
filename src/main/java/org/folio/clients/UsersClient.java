package org.folio.clients;

import static org.folio.util.ExternalPathResolver.USERS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithId;

import io.vertx.core.json.JsonObject;
import java.util.Optional;
import org.folio.util.OkapiConnectionParams;
import org.springframework.stereotype.Component;

@Component
public class UsersClient {
  public Optional<JsonObject> getById(String userId, OkapiConnectionParams params) {
    String endpoint = ClientUtil.buildQueryEndpoint(resourcesPathWithId(USERS), params.getOkapiUrl(), userId);
    return ClientUtil.getRequest(params, endpoint);
  }
}
