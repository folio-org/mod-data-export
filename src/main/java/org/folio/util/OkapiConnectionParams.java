package org.folio.util;

import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

public class OkapiConnectionParams {
  private String okapiUrl;
  private String tenantId;
  private String token;
  private Map<String, String> headers;

  public OkapiConnectionParams() {
  }

  public OkapiConnectionParams(Map<String, String> okapiHeaders) {
    this.okapiUrl = okapiHeaders.getOrDefault("x-okapi-url", "localhost");
    this.tenantId = okapiHeaders.getOrDefault(OKAPI_HEADER_TENANT, "");
    this.token = okapiHeaders.getOrDefault(OKAPI_HEADER_TOKEN, "dummy");
    this.headers = okapiHeaders;
  }

  public String getOkapiUrl() {
    return okapiUrl;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getToken() {
    return token;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }
}
