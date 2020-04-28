package org.folio.clients;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Component
public class InventoryClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GET_INSTANCES_URL = "%s/instance-storage/instances?query=(%s)";
  private static final String QUERY_PATTERN_INVENTORY = "id==%s";
  private static final String LIMIT_PATTERN = "&limit=";

  private static final int SETTING_LIMIT = 100;
  private static final String NATURE_OF_CONTENT_TERMS_URL = "%s/nature-of-content-terms?limit=" + SETTING_LIMIT;
  private static final String NATURE_OF_CONTENT_TERMS_FIELD = "natureOfContentTerms";

  public Optional<JsonObject> getInstancesByIds(List<String> ids, OkapiConnectionParams params, int partitionSize) {
    return ClientUtil.getByIds(ids, params, GET_INSTANCES_URL + LIMIT_PATTERN + partitionSize, QUERY_PATTERN_INVENTORY);
  }

  public List<JsonObject> getNatureOfContentTerms(OkapiConnectionParams params) {
    return getSettingsByUrl(NATURE_OF_CONTENT_TERMS_URL, params, NATURE_OF_CONTENT_TERMS_FIELD);
  }

  private List<JsonObject> getSettingsByUrl(String url, OkapiConnectionParams params, String field) {
    List<JsonObject> result = new ArrayList<>();
    HttpGet httpGet = new HttpGet();
    ClientUtil.setCommonHeaders(httpGet, params);
    httpGet.setURI(URI.create(format(url, params.getOkapiUrl())));
    try (CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet)) {
      HttpEntity httpEntity = response.getEntity();
      JsonObject jsonEntity = new JsonObject(EntityUtils.toString(httpEntity));
      if (jsonEntity.containsKey(field)) {
        JsonArray array = jsonEntity.getJsonArray(field);
        for (Object element : array) {
          result.add(JsonObject.mapFrom(element));
        }
      }
    } catch (IOException e) {
      LOGGER.error("Exception while calling {}", httpGet.getURI(), e);
    }
    return result;
  }

}
