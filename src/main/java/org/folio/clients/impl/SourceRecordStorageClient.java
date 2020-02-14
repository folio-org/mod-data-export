package org.folio.clients.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.folio.clients.SynchronousOkapiClient;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.vertx.core.json.JsonObject;

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
  public static final String GET_RECORD_PATTERN = "%s/source-storage/formattedRecords/%s?identifier=INSTANCE";

  public SourceRecordStorageClient(OkapiConnectionParams okapiConnectionParams) {
    super(okapiConnectionParams);
  }

  @Override
  protected void prepareRequest(HttpRequestBase requestBase, String id) {
    requestBase.setURI(URI.create(String.format(GET_RECORD_PATTERN,
      okapiConnectionParams.getOkapiUrl(), id)));
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
