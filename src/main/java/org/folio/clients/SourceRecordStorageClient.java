package org.folio.clients;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.folio.util.ExternalPathResolver;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.folio.clients.ClientUtil.getResponseEntity;
import static org.folio.util.ExternalPathResolver.SRS;

@Component
public class SourceRecordStorageClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GET_RECORDS_QUERY = ExternalPathResolver.resourcesPathWithPrefix(SRS) + "?idType=INSTANCE";
  @Autowired
  private ErrorLogService errorLogService;

  public Optional<JsonObject> getRecordsByInstanceIds(List<String> ids, String jobExecutionId, OkapiConnectionParams params) {
    HttpPost httpPost = new HttpPost(format(GET_RECORDS_QUERY, params.getOkapiUrl()));
    String body = new JsonArray(ids).encode();
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      httpPost.setEntity(new StringEntity(body));
      ClientUtil.setCommonHeaders(httpPost, params);
      CloseableHttpResponse response = client.execute(httpPost);
      return Optional.ofNullable(getResponseEntity(response));
    } catch (IOException e) {
      LOGGER.error("Exception while calling {}", httpPost.getURI(), e);
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_CALLING_URI.getCode(), Arrays.asList(httpPost.getURI().toString(), e.getMessage()), jobExecutionId, params.getTenantId());
      return Optional.empty();
    }
  }
}
