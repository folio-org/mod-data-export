package org.folio.dataexp.util;

import org.folio.okapi.common.XOkapiHeaders;

import java.util.Map;

public class OkapiConnectionParams {
  private String okapiUrl;
  private String tenantId;
  private String token;
  private Map<String, String> headers;

  public OkapiConnectionParams() {
  }

  public OkapiConnectionParams(Map<String, String> okapiHeaders) {
    this.okapiUrl = okapiHeaders.getOrDefault(XOkapiHeaders.URL, "localhost");
    this.tenantId = okapiHeaders.getOrDefault(XOkapiHeaders.TENANT, "");
    this.token = okapiHeaders.getOrDefault(XOkapiHeaders.TOKEN, "dummy");
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
