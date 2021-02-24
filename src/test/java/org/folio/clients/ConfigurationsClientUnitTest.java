package org.folio.clients;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.RestVerticleTestBase.TENANT_ID;
import static org.folio.rest.jaxrs.model.ErrorLog.LogLevel.ERROR;
import static org.folio.util.ErrorCode.ERROR_QUERY_HOST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.service.logs.ErrorLogServiceImpl;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class ConfigurationsClientUnitTest {

  private static OkapiConnectionParams okapiConnectionParams;

  @Spy
  @InjectMocks
  private ConfigurationsClient configurationsClient;
  @Mock
  private ErrorLogServiceImpl errorLogService;

  @BeforeAll
  public static void beforeClass() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put("x-okapi-url", "localhost");
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void shouldSaveGeneralErrorForHost() throws HttpClientException {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("configs", new JsonArray());
    doReturn(Optional.of(jsonObject)).when(configurationsClient)
      .getResponseFromGetRequest(anyString(), any(OkapiConnectionParams.class));
    when(errorLogService.getByQuery(any(Criterion.class), eq(okapiConnectionParams.getTenantId())))
      .thenReturn(Future.succeededFuture(
        Collections.emptyList()));

    configurationsClient.getInventoryRecordLink(EMPTY, EMPTY, okapiConnectionParams);

    Mockito.verify(errorLogService).saveGeneralError(anyString(), anyString(), anyString());
  }

  @Test
  void shouldNotSaveGeneralErrorForHost_whenErrorIsAlreadyPresent() throws HttpClientException {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("configs", new JsonArray());
    doReturn(Optional.of(jsonObject)).when(configurationsClient)
      .getResponseFromGetRequest(anyString(), any(OkapiConnectionParams.class));
    when(errorLogService.getByQuery(any(Criterion.class), eq(okapiConnectionParams.getTenantId())))
      .thenReturn(Future.succeededFuture(
        getMockErrorLogList()));

    configurationsClient.getInventoryRecordLink(EMPTY, EMPTY, okapiConnectionParams);

    Mockito.verify(errorLogService, never()).saveGeneralError(anyString(), anyString(), anyString());
  }

  private List<ErrorLog> getMockErrorLogList() {
    ErrorLog errorLog = new ErrorLog()
      .withId(UUID.randomUUID().toString())
      .withJobExecutionId(UUID.randomUUID().toString())
      .withLogLevel(ERROR)
      .withErrorMessageCode(ERROR_QUERY_HOST.getCode())
      .withCreatedDate(new Date())
      .withMetadata(new Metadata()
        .withCreatedByUserId(UUID.randomUUID().toString())
        .withUpdatedByUserId(UUID.randomUUID().toString()));
    return Collections.singletonList(errorLog);
  }

}
