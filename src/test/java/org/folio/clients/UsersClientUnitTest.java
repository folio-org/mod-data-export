package org.folio.clients;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.folio.rest.HttpServerTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@RunWith(MockitoJUnitRunner.class)
public class UsersClientUnitTest extends HttpServerTestBase {

  @Test
  public void shouldReturnUserById() throws IOException {
    // given
    String instancesJson = IOUtils.toString(new FileReader("src/test/resources/clients/userResponse.json"));
    String userId = UUID.randomUUID().toString();
    JsonObject data = new JsonObject(instancesJson);
    WireMock.stubFor(get(urlEqualTo("/users/" + userId))
      .willReturn(WireMock.ok().withBody(Json.encode(data))));
    UsersClient usersClient = new UsersClient();
    // when
    Optional<JsonObject> optionalUser = usersClient.getById(userId, okapiConnectionParams);
    // then
    Assert.assertTrue(optionalUser.isPresent());
  }
}
