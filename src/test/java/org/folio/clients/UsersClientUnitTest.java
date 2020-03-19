package org.folio.clients;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.IOUtils;
import org.folio.rest.HttpServerTestBase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RunWith(VertxUnitRunner.class)
public class UsersClientUnitTest extends HttpServerTestBase {

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() throws IOException {
    String json = IOUtils.toString(new FileReader("src/test/resources/clients/userResponse.json"));
    JsonObject data = new JsonObject(json);
    router.route("/users/:id").method(HttpMethod.GET).handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(data.toBuffer());
    });
  }

  @Test
  public void shouldReturnUserById() {
    // given
    UsersClient usersClient = new UsersClient();
    // when
    Optional<JsonObject> optionalUser = usersClient.getById(UUID.randomUUID().toString(), okapiConnectionParams);
    // then
    Assert.assertTrue(optionalUser.isPresent());
  }
}
