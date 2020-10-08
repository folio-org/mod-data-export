package org.folio.clients;

import static java.lang.String.format;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ClientUtil() {
  }

  public static JsonObject getByIds(List<String> ids, OkapiConnectionParams params, String endpoint, String queryPattern) throws HttpClientException {
    HttpGet httpGet = new HttpGet();
    setCommonHeaders(httpGet, params);
    URI uri = prepareFullUriWithQuery(ids, params, endpoint, queryPattern);
    httpGet.setURI(uri);
    LOGGER.info("Calling GET By IDs {}", uri);
    try (CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet)) {
      return getResponseEntity(response);
    } catch (IOException exception) {
      LOGGER.error("Exception while calling {}", httpGet.getURI(), exception);
      throw new HttpClientException(format("Exception while calling %s, message: %s", httpGet.getURI(), exception.getMessage()));
    }
  }

  public static JsonObject getRequest(OkapiConnectionParams params, String endpoint) throws HttpClientException {
    HttpGet httpGet = new HttpGet();
    setCommonHeaders(httpGet, params);
    httpGet.setURI(URI.create(endpoint));
    LOGGER.info("Calling GET {}", endpoint);
    try (CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet)) {
      return getResponseEntity(response);
    } catch (IOException exception) {
      LOGGER.error("Exception while calling {}", httpGet.getURI(), exception);
      throw new HttpClientException(format("Exception while calling %s, message: %s", httpGet.getURI(), exception.getMessage()));
    }
  }

  public static void setCommonHeaders(HttpRequestBase requestBase, OkapiConnectionParams params) {
    requestBase.setHeader(OKAPI_HEADER_TOKEN, params.getToken());
    requestBase.setHeader(OKAPI_HEADER_TENANT, params.getTenantId());
    requestBase.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    requestBase.setHeader((HttpHeaders.ACCEPT), MediaType.APPLICATION_JSON);
  }

  @NotNull
  private static URI prepareFullUriWithQuery(List<String> ids, OkapiConnectionParams params, String endpoint, String queryPattern) {
    String query = ids.stream().map(s -> format(queryPattern, s)).collect(Collectors.joining(" or "));
    try {
      String uri = format(endpoint, params.getOkapiUrl(), URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
      return URI.create(uri);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Exception while building a query from list of ids", e);
    }
  }

  public static JsonObject getResponseEntity(CloseableHttpResponse response) throws IOException {
    HttpEntity entity = response.getEntity();
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entity != null) {
      try {
        return new JsonObject(EntityUtils.toString(entity));
      } catch (IOException e) {
        LOGGER.error("Exception while building response entity", e);
      }
    }
    throw new IOException("Get invalid response with status: " + response.getStatusLine().getStatusCode());
  }

  static String buildQueryEndpoint(String endpoint, Object... params) {
    return format(endpoint, params);
  }
}
