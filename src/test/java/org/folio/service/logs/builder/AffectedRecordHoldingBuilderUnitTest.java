package org.folio.service.logs.builder;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.folio.clients.ConfigurationsClient;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.service.ApplicationTestConfig;
import org.folio.service.logs.AffectedRecordHoldingBuilder;
import org.folio.service.logs.AffectedRecordInstanceBuilder;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Objects;

import static org.folio.TestUtil.readFileContentFromResources;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.RestVerticleTestBase.TENANT_ID;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.HOLDINGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class AffectedRecordHoldingBuilderUnitTest {

  private static final String INSTANCE_ID = "a89eccf0-57a6-495e-898d-32b9b2210f2f";
  private static final String HOLDING_ID = "67cd0046-e4f1-4e4f-9024-adf0b0039d09";
  private static final String INSTANCE_HR_ID = "inst000000000017";
  private static final String HOLDING_HR_ID = "hold000000000007";
  private static final String INSTANCE_TITLE = "Interesting Times";
  private static OkapiConnectionParams params;
  @Spy
  @InjectMocks
  private AffectedRecordHoldingBuilder affectedRecordBuilder;
  @Mock
  private AffectedRecordInstanceBuilder affectedRecordInstanceBuilder;
  @Mock
  private ConfigurationsClient configurationsClient;

  public AffectedRecordHoldingBuilderUnitTest() {
    Context vertxContext = Vertx.vertx().getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationTestConfig.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @BeforeAll
  static void beforeAll() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    params = new OkapiConnectionParams(headers);
  }

  @Test
  void buildAffectedRecord_shouldReturnAffectedHoldingRecord_instanceAsRelated() {
    // given
    String expectedLink = "localhost:3000/inventory/view/a89eccf0-57a6-495e-898d-32b9b2210f2f/67cd0046-e4f1-4e4f-9024-adf0b0039d09";
    JsonObject record = new JsonObject(readFileContentFromResources("mapping/given_InstanceHoldingsItems.json"));
    Mockito.when(configurationsClient.getInventoryRecordLink(anyString(), anyString(), any(OkapiConnectionParams.class)))
      .thenReturn(expectedLink);
    doCallRealMethod().when(affectedRecordInstanceBuilder).build(record, "jobId", INSTANCE_ID, false, params);
    // when
    AffectedRecord affectedRecord = affectedRecordBuilder.build(record, "jobId", HOLDING_ID, true, params);

    // then
    assertEquals(HOLDING_ID, affectedRecord.getId());
    assertEquals(HOLDING_HR_ID, affectedRecord.getHrid());
    assertEquals(HOLDINGS, affectedRecord.getRecordType());
    assertEquals(expectedLink, affectedRecord.getInventoryRecordLink());
    assertEquals(1, affectedRecord.getAffectedRecords().size());
    assertTrue(Objects.isNull(affectedRecord.getTitle()));
    Mockito.verify(configurationsClient, times(1))
      .getInventoryRecordLink(anyString(), anyString(), any(OkapiConnectionParams.class));
  }

  @Test
  void buildAffectedRecord_shouldReturnRecordWithoutLink_IfClientReturnEmpty() {
    // given
    JsonObject record = new JsonObject(readFileContentFromResources("mapping/given_InstanceHoldingsItems.json"));
    Mockito.when(configurationsClient.getInventoryRecordLink(anyString(), anyString(), any(OkapiConnectionParams.class)))
      .thenReturn(StringUtils.EMPTY);

    // when
    AffectedRecord affectedRecord = affectedRecordBuilder.build(record, "jobId", HOLDING_ID, true, params);

    // then
    assertEquals(HOLDING_ID, affectedRecord.getId());
    assertEquals(HOLDING_HR_ID, affectedRecord.getHrid());
    assertEquals(HOLDINGS, affectedRecord.getRecordType());
    assertNull(affectedRecord.getInventoryRecordLink());
    Mockito.verify(configurationsClient, times(1))
      .getInventoryRecordLink(anyString(), anyString(), any(OkapiConnectionParams.class));
  }

  @Test
  void
  buildAffectedRecord_shouldReturnAffectedHoldingRecordWithoutLink_IfLinkCreationNotRequired() {
    // given
    JsonObject record = new JsonObject(readFileContentFromResources("mapping/given_InstanceHoldingsItems.json"));
    doCallRealMethod().when(affectedRecordInstanceBuilder).build(record, "jobId", INSTANCE_ID, false, params);

    // when
    AffectedRecord affectedRecord = affectedRecordBuilder.build(record, "jobId", HOLDING_ID, false, params);

    // then
    assertEquals(HOLDING_ID, affectedRecord.getId());
    assertEquals(HOLDING_HR_ID, affectedRecord.getHrid());
    assertEquals(HOLDINGS, affectedRecord.getRecordType());
    assertTrue(Objects.isNull(affectedRecord.getInventoryRecordLink()));
    verify(configurationsClient, never()).getInventoryRecordLink(anyString(), anyString(), any(OkapiConnectionParams.class));
    assertEquals(1, affectedRecord.getAffectedRecords().size());
    assertTrue(Objects.isNull(affectedRecord.getTitle()));
  }
}
