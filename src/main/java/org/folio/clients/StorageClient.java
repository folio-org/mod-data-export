package org.folio.clients;

import io.vertx.core.json.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.folio.util.OkapiConnectionParams;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class StorageClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GET_RECORDS_PATTERN_SRS = "%s/source-storage/records?query=(%s)";
  private static final String QUERY_PATTERN_SRS = "externalIdsHolder.instanceId==%s";
  private static final String GET_INSTANCE_PATTERN_INVENTORY = "%s/instance-storage/instances?query=(%s)";
  private static final String QUERY_PATTERN_INVENTORY = "id==%s";
  private static final String LIMIT_PATTERN = "&limit=";

  public Optional<JsonObject> getByIdsFromSRS(List<String> ids, OkapiConnectionParams params, int partitionSize) {
    return getByIds(ids, params, GET_RECORDS_PATTERN_SRS+ LIMIT_PATTERN + partitionSize, QUERY_PATTERN_SRS);
  }

  public Optional<JsonObject> getByIdsFromInventory(List<String> ids, OkapiConnectionParams params, int partitionSize) {
    return getByIds(ids, params, GET_INSTANCE_PATTERN_INVENTORY + LIMIT_PATTERN + partitionSize, QUERY_PATTERN_INVENTORY);
  }

  private Optional<JsonObject> getByIds(List<String> ids, OkapiConnectionParams params, String pattern, String queryPattern) {
    HttpGet httpGet = new HttpGet();
    ClientUtil.setCommonHeaders(httpGet, params);
    httpGet.setURI(prepareFullUri(ids, params, pattern, queryPattern));
    try (CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet)) {
      return Optional.ofNullable(getResponseEntity(response));
    } catch (IOException e) {
      LOGGER.error("Exception while calling {}", httpGet.getURI(), e);
      return Optional.empty();
    }
  }

  @NotNull
  private URI prepareFullUri(List<String> ids, OkapiConnectionParams params, String recordPattern, String queryPattern) {
    String query = ids.stream().map(s -> String.format(queryPattern, s)).collect(Collectors.joining(" or "));
    try {
      String uri = String.format(recordPattern, params.getOkapiUrl(), URLEncoder.encode(query, "UTF-8"));
      return URI.create(uri);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Exception while building a query from list of ids", e);
    }
  }

  private JsonObject getResponseEntity(CloseableHttpResponse response) {
    HttpEntity entity = response.getEntity();
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entity != null) {
      try {
        return new JsonObject(EntityUtils.toString(entity));
      } catch (IOException e) {
        LOGGER.error("Exception while requesting instances", e);
      }
    }
    return null;
  }
}
