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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@Component
public class InventoryClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GET_INSTANCES_URL = "%s/instance-storage/instances?query=(%s)";
  private static final String QUERY_PATTERN_INVENTORY = "id==%s";
  private static final String LIMIT_PATTERN = "&limit=";

  private static final int SETTING_LIMIT = 200;
  private static final String NATURE_OF_CONTENT_TERMS_URL = "%s/nature-of-content-terms?limit=" + SETTING_LIMIT;
  private static final String NATURE_OF_CONTENT_TERMS_FIELD = "natureOfContentTerms";

  public Optional<JsonObject> getInstancesByIds(List<String> ids, OkapiConnectionParams params, int partitionSize) {
    return ClientUtil.getByIds(ids, params, GET_INSTANCES_URL + LIMIT_PATTERN + partitionSize, QUERY_PATTERN_INVENTORY);
  }

  public Map<String, JsonObject> getNatureOfContentTerms(OkapiConnectionParams params) {
    return getSettingsByUrl(NATURE_OF_CONTENT_TERMS_URL, params, NATURE_OF_CONTENT_TERMS_FIELD);
  }

  private Map<String, JsonObject> getSettingsByUrl(String url, OkapiConnectionParams params, String field) {
    Map<String, JsonObject> map = new HashMap<>();
    HttpGet httpGet = new HttpGet();
    ClientUtil.setCommonHeaders(httpGet, params);
    httpGet.setURI(URI.create(format(url, params.getOkapiUrl())));
    try (CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet)) {
      HttpEntity httpEntity = response.getEntity();
      JsonObject responseBody = new JsonObject(EntityUtils.toString(httpEntity));
      if (responseBody.containsKey(field)) {
        JsonArray array = responseBody.getJsonArray(field);
        for (Object item : array) {
          JsonObject jsonItem = JsonObject.mapFrom(item);
          map.put(jsonItem.getString("id"), jsonItem);
        }
      }
    } catch (IOException e) {
      LOGGER.error("Exception while calling {}", httpGet.getURI(), e);
    }
    return map;
  }

}
