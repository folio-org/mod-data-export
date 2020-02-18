package org.folio.clients;

import io.vertx.core.json.JsonObject;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

/**
 * Base class for okapi clients that use blocking method for http calls.
 */
public abstract class SynchronousOkapiClient {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected final OkapiConnectionParams okapiConnectionParams;
  private final CloseableHttpClient httpClient;

  public SynchronousOkapiClient(OkapiConnectionParams okapiConnectionParams) {
    this.okapiConnectionParams = okapiConnectionParams;
    httpClient = HttpClients.createDefault();
  }

  public Optional<JsonObject> getByIds(List<String> ids) {
    HttpGet httpGet = new HttpGet();
    setCommonHeaders(httpGet);
    prepareRequest(httpGet, ids);

    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      return Optional.ofNullable(postProcess(response));
    } catch (IOException e) {
      log.error("Exception while calling " + httpGet.getURI(), e);
      return Optional.empty();
    }
  }

  protected void setCommonHeaders(HttpRequestBase requestBase) {
    Map<String, String> headers = okapiConnectionParams.getHeaders();
    requestBase.setHeader(OKAPI_HEADER_TOKEN, headers.get(OKAPI_HEADER_TOKEN));
    requestBase.setHeader(OKAPI_HEADER_TENANT, headers.get(OKAPI_HEADER_TENANT));
    requestBase.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    requestBase.setHeader((HttpHeaders.ACCEPT), MediaType.APPLICATION_JSON);
  }

  protected abstract void prepareRequest(HttpRequestBase requestBase, List<String> id);

  protected abstract JsonObject postProcess(CloseableHttpResponse response);

}
