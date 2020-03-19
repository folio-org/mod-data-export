package org.folio.clients;

import io.vertx.core.json.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
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

/**
 * The client that synchronously communicates with the Source Record Storage(SRS).
 *
 * @link https://github.com/folio-org/mod-source-record-storage
 */
@Component
public class SourceRecordStorageClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GET_RECORDS_PATTERN = "%s/source-storage/records?query=(%s)";
  private static final String QUERY_PATTERN = "externalIdsHolder.instanceId==%s";

  public Optional<JsonObject> getByIds(List<String> ids, OkapiConnectionParams params) {
    HttpGet httpGet = new HttpGet();
    ClientUtil.setCommonHeaders(httpGet, params);
    httpGet.setURI(prepareFullUri(ids, params));
    try (CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet)) {
      return Optional.ofNullable(getResponseEntity(response));
    } catch (IOException e) {
      LOGGER.error("Exception while calling {}",httpGet.getURI(), e);
      return Optional.empty();
    }
  }

  @NotNull
  private URI prepareFullUri(List<String> ids, OkapiConnectionParams params) {
    String query = ids.stream().map(s -> String.format(QUERY_PATTERN, s)).collect(Collectors.joining(" or "));
    try {
      String uri = String.format(GET_RECORDS_PATTERN, params.getOkapiUrl(), URLEncoder.encode(query, "UTF-8"));
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
        LOGGER.error("Exception while requesting SRS", e);
      }
    }
    return null;
  }
}
