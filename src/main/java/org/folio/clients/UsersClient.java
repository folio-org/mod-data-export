package org.folio.clients;

import io.vertx.core.json.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.folio.HttpStatus;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

@Component
public class UsersClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GET_USER_URL = "%s/users/%s";

  public Optional<JsonObject> getById(String userId, OkapiConnectionParams params) {
    HttpGet httpGet = new HttpGet();
    ClientUtil.setCommonHeaders(httpGet, params);
    httpGet.setURI(URI.create(String.format(GET_USER_URL, params.getOkapiUrl(), userId)));
    try (CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet)) {
      HttpEntity entity = response.getEntity();
      HttpStatus httpStatus = HttpStatus.get(response.getStatusLine().getStatusCode());
      if (HttpStatus.HTTP_OK == httpStatus) {
        JsonObject userJsonObject = new JsonObject(EntityUtils.toString(entity));
        return Optional.of(userJsonObject);
      } else {
        return Optional.empty();
      }
    } catch (IOException e) {
      LOGGER.error("Exception while calling {}", httpGet.getURI(), e);
      return Optional.empty();
    }
  }
}
