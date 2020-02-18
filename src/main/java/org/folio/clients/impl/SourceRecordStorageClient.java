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

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String GET_RECORDS_PATTERN = "%s/source-storage/records?query=(%s)";
  public static final String QUERY_PATTERN = "externalIdsHolder.instanceId==%s";

  public SourceRecordStorageClient(OkapiConnectionParams okapiConnectionParams) {
    super(okapiConnectionParams);
  }

  @Override
  protected void prepareRequest(HttpRequestBase requestBase, List<String> ids) {
    requestBase.setURI(prepareFullUri(ids));
  }

  @NotNull
  private URI prepareFullUri(List<String> ids) {
    String query = ids.stream().map(s -> String.format(QUERY_PATTERN, s))
      .collect(Collectors.joining(" or "));
    String uriString = "";
    try {
      uriString = String.format(GET_RECORDS_PATTERN,
        okapiConnectionParams.getOkapiUrl(), URLEncoder.encode(query, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      log.error("Exception while building a query from list of ids", e);
    }
    return URI.create(uriString);
  }

  @Override
  protected JsonObject postProcess(CloseableHttpResponse response) {
    HttpEntity entity = response.getEntity();
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entity != null) {
      try {
        return new JsonObject(EntityUtils.toString(entity));
      } catch (IOException e) {
        log.error("Exception while requesting SRS", e);
      }
    }
    return null;
  }
}
