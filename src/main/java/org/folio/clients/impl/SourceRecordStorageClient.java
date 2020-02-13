package org.folio.clients.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.folio.clients.SynchronousOkapiClient;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.vertx.core.json.JsonObject;

@Component
public class SourceRecordStorageClient extends SynchronousOkapiClient {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String MOD_SRS_PATTERN = "%s/source-storage/formattedRecords/%s?identifier=INSTANCE";

  public SourceRecordStorageClient(OkapiConnectionParams okapiConnectionParams) {
    super(okapiConnectionParams);
  }

  @Override
  public Optional<JsonObject> getById(String uuid) {
    HttpGet httpGet = new HttpGet(String.format(MOD_SRS_PATTERN,
      okapiConnectionParams.getOkapiUrl(), uuid));
    try (CloseableHttpClient httpClient = HttpClients.createDefault();
         CloseableHttpResponse response = httpClient.execute(httpGet)) {
      HttpEntity entity = response.getEntity();
      if (response.getStatusLine().getStatusCode() == 200 && entity != null) {
        JsonObject entries = new JsonObject(EntityUtils.toString(entity));
        return Optional.of(entries);
      }
    } catch (IOException e) {
      log.error("Exception while loading records from SRS ", e);
      return Optional.empty();
    }
    return Optional.empty();
  }
}
