package org.folio.clients;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpRequestBase;
import org.folio.util.OkapiConnectionParams;

import javax.ws.rs.core.MediaType;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

public final class ClientUtil {

  private ClientUtil() {
  }

  public static void setCommonHeaders(HttpRequestBase requestBase, OkapiConnectionParams params) {
    requestBase.setHeader(OKAPI_HEADER_TOKEN, params.getToken());
    requestBase.setHeader(OKAPI_HEADER_TENANT, params.getTenantId());
    requestBase.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    requestBase.setHeader((HttpHeaders.ACCEPT), MediaType.APPLICATION_JSON);
  }
}
