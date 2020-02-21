package org.folio.clients.impl;

import io.vertx.core.json.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.folio.clients.SynchronousOkapiClient;
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
import java.util.stream.Collectors;

/**
 * The client that synchronously communicates with the Source Record Storage(SRS).
 * <p>
 * Retrieves collection of underlying SRS records as a source of truth.
 * Retrieves collection of Inventory records that do not have underlying SRS records
 *
 * @link https://github.com/folio-org/mod-source-record-storage
 */
@Component
public class SourceRecordStorageClient extends SynchronousOkapiClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String GET_RECORDS_PATTERN = "%s/source-storage/records?query=(%s)";
  public static final String QUERY_PATTERN = "externalIdsHolder.instanceId==%s";

  @Override
  protected void prepareRequest(HttpRequestBase requestBase, List<String> ids, OkapiConnectionParams params) {
    requestBase.setURI(prepareFullUri(ids, params));
  }

  @NotNull
  private URI prepareFullUri(List<String> ids, OkapiConnectionParams params) {
    String query = ids.stream().map(s -> String.format(QUERY_PATTERN, s)).collect(Collectors.joining(" or "));
    try {
      String uri = String.format(GET_RECORDS_PATTERN, params.getOkapiUrl(), URLEncoder.encode(query, "UTF-8"));
      return URI.create(uri);
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Exception while building a query from list of ids", e);
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  protected JsonObject postProcess(CloseableHttpResponse response) {
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
