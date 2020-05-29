package org.folio.clients;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.TestUtil;
import org.folio.rest.HttpServerTestBase;
import org.folio.rest.jaxrs.model.UserInfo;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class UsersClientUnitTest extends HttpServerTestBase {
  protected static final String USERS_BY_ID_URL = "/users/:id";
  protected static final String USER_RESPONSE_JSON = "mockData/user/get_user_response.json";

  @BeforeAll
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() {
    String json = TestUtil.readFileContentFromResources(USER_RESPONSE_JSON);
    JsonObject data = new JsonObject(json);
    router.route(USERS_BY_ID_URL).method(HttpMethod.GET).handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(data.toBuffer());
    });
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
