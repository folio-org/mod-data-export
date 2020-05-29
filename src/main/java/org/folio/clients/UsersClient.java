package org.folio.clients;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.UserInfo;
import org.folio.util.OkapiConnectionParams;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.folio.util.ExternalPathResolver.USERS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithId;
import static org.folio.util.ExternalPathResolver.resourcesPathWithSuffix;

@Component
public class UsersClient {
  private static final String PERSONAL = "personal";
  private static final String FIRST_NAME = "firstName";
  private static final String LAST_NAME = "lastName";
  private static final String USER_NAME = "username";

  public Optional<JsonObject> getById(String userId, OkapiConnectionParams params) {
    String endpoint = ClientUtil.buildQueryEndpoint(resourcesPathWithId(USERS), params.getOkapiUrl(), userId);
    return ClientUtil.getRequest(params, endpoint);
  }

  public Future<UserInfo> getUserInfoAsync(String userId, OkapiConnectionParams params) {
    Promise<UserInfo> promise = Promise.promise();
    String endpoint = ClientUtil.buildQueryEndpoint(resourcesPathWithSuffix(USERS), userId);
    ClientUtilAsync.getRequest(endpoint, params).onComplete(user -> {
      if (user.failed()) {
        promise.fail(user.cause());
      } else {
        JsonObject userPersonalInfo = user.result().getJsonObject(PERSONAL);
        UserInfo userInfo = new UserInfo()
          .withFirstName(userPersonalInfo.getString(FIRST_NAME))
          .withLastName(userPersonalInfo.getString(LAST_NAME))
          .withUserName(user.result().getString(USER_NAME));
        promise.complete(userInfo);
      }
    });
    return promise.future();
  }
}
