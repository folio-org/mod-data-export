package org.folio.rest;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.folio.util.OkapiConnectionParams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

/**
 * Class for tests that base on testing code using HTTP mock server
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class HttpServerTestBase {

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String HOST = "http://localhost:";
  protected static OkapiConnectionParams okapiConnectionParams;

  @Rule
  public WireMockRule snapshotMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true))
  );

  @Before
  public void setUpOkapiParams() {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put("x-okapi-url", HOST + snapshotMockServer.port());
    okapiHeaders.put(OKAPI_HEADER_TENANT, TENANT_ID);
    okapiHeaders.put(OKAPI_HEADER_TOKEN, TOKEN);
    okapiConnectionParams = new OkapiConnectionParams(okapiHeaders);
  }

}
