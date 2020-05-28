package org.folio.clients;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.UserInfo;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.util.ExternalPathResolver.USERS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithId;

@Component
public class UsersClient {
  private static final String PERSONAL = "personal";
  private static final String FIRST_NAME = "firstName";
  private static final String LAST_NAME = "lastName";
  private static final String USER_NAME = "username";

  private ClientAsync clientUtilAsync;

  public UsersClient(@Autowired ClientAsync clientUtilAsync) {
    this.clientUtilAsync = clientUtilAsync;
  }

  public Optional<JsonObject> getById(String userId, OkapiConnectionParams params) {
    String endpoint = ClientUtil.buildQueryEndpoint(resourcesPathWithId(USERS), params.getOkapiUrl(), userId);
    return ClientUtil.getRequest(params, endpoint);
  }

  public Future<UserInfo> getUserInfoAsync(String userId, OkapiConnectionParams params) {
    String endpoint = ClientUtil.buildQueryEndpoint(resourcesPathWithId(USERS), params.getOkapiUrl(), userId);
    return clientUtilAsync.getRequest(endpoint, params).compose(user -> {
      JsonObject userPersonalInfo = user.getJsonObject(PERSONAL);
      UserInfo userInfo = new UserInfo()
        .withFirstName(userPersonalInfo.getString(FIRST_NAME))
        .withLastName(userPersonalInfo.getString(LAST_NAME))
        .withUserName(user.getString(USER_NAME));
      return succeededFuture(userInfo);
    });
  }

}
