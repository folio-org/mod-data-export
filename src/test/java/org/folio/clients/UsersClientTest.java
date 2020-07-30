package org.folio.clients;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.impl.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.UserInfo;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class UsersClientTest extends RestVerticleTestBase {
  private static OkapiConnectionParams okapiConnectionParams;

  @BeforeAll
  public static void beforeClass() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void shouldReturnUserById() {
    // given
    UsersClient usersClient = new UsersClient();
    // when
    Optional<JsonObject> optionalUser = usersClient.getById(UUID.randomUUID().toString(), okapiConnectionParams);
    // then
    Assert.assertTrue(optionalUser.isPresent());
  }

  @Test
  void shouldReturnUserInfoById(VertxTestContext context) {
    // given
    UsersClient usersClient = new UsersClient();
    // when
    Future<UserInfo> userInfo = usersClient.getUserInfoAsync(UUID.randomUUID().toString(), okapiConnectionParams);
    // then
    userInfo.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.succeeded());
        context.completeNow();
      }));
  }
}
