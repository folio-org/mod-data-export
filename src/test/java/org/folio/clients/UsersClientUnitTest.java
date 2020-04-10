package org.folio.clients;

import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.folio.rest.HttpServerTestBase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class UsersClientUnitTest extends HttpServerTestBase {

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() throws IOException {
    String json = IOUtils.toString(new FileReader("src/test/resources/clients/userResponse.json"));
    JsonObject data = new JsonObject(json);
    httpServer.requestHandler(request -> {
      request.response().putHeader("content-type", "application/json");
      if (request.path().contains("/users/")) {
        request.response().end(data.toBuffer());
      }
    }).listen(port);
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
