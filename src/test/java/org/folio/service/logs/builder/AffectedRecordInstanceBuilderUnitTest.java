package org.folio.service.logs.builder;

import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.folio.clients.ConfigurationsClient;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.service.logs.AffectedRecordInstanceBuilder;
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
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class AffectedRecordInstanceBuilderUnitTest {

  private static final String INSTANCE_ID = "a89eccf0-57a6-495e-898d-32b9b2210f2f";
  private static final String INSTANCE_HR_ID = "inst000000000017";
  private static final String INSTANCE_TITLE = "Interesting Times";
  private static OkapiConnectionParams params;

  @Spy
  @InjectMocks
  private AffectedRecordInstanceBuilder affectedRecordBuilder;
  @Mock
  private ConfigurationsClient configurationsClient;

  @BeforeAll
  static void beforeAll() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    params = new OkapiConnectionParams(headers);
  }

  @Test
  void buildAffectedRecord_shouldReturnRecordOnlyWithInstance() {
    // given
    String expectedLink = "localhost:3000/inventory/view/a89eccf0-57a6-495e-898d-32b9b2210f2f";
    JsonObject record = new JsonObject(readFileContentFromResources("mapping/given_InstanceHoldingsItems.json"));
    Mockito.when(configurationsClient.getInventoryRecordLink(anyString(), anyString(), any(OkapiConnectionParams.class)))
      .thenReturn(expectedLink);

    // when
    AffectedRecord affectedRecord = affectedRecordBuilder.build(record, "jobId", INSTANCE_ID, true, params);

    // then
    assertEquals(INSTANCE_TITLE, affectedRecord.getTitle());
    assertEquals(INSTANCE_ID, affectedRecord.getId());
    assertEquals(INSTANCE_HR_ID, affectedRecord.getHrid());
    assertEquals(INSTANCE, affectedRecord.getRecordType());
    assertEquals(expectedLink, affectedRecord.getInventoryRecordLink());
    assertTrue(affectedRecord.getAffectedRecords().isEmpty());
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
    AffectedRecord affectedRecord = affectedRecordBuilder.build(record, "jobId", INSTANCE_ID, true, params);

    // then
    assertEquals(INSTANCE_TITLE, affectedRecord.getTitle());
    assertEquals(INSTANCE_ID, affectedRecord.getId());
    assertEquals(INSTANCE_HR_ID, affectedRecord.getHrid());
    assertEquals(INSTANCE, affectedRecord.getRecordType());
    assertNull(affectedRecord.getInventoryRecordLink());
    assertTrue(affectedRecord.getAffectedRecords().isEmpty());
    Mockito.verify(configurationsClient, times(1))
      .getInventoryRecordLink(anyString(), anyString(), any(OkapiConnectionParams.class));
  }

  @Test
  void buildAffectedRecord_shouldReturnRecordWithoutLink_IfLinkCreationNotRequired() {
    // give

    JsonObject record = new JsonObject(readFileContentFromResources("mapping/given_InstanceHoldingsItems.json"));

    // when
    AffectedRecord affectedRecord = affectedRecordBuilder.build(record, "jobId", INSTANCE_ID, false, params);

    // then
    assertEquals(INSTANCE_TITLE, affectedRecord.getTitle());
    assertEquals(INSTANCE_ID, affectedRecord.getId());
    assertEquals(INSTANCE_HR_ID, affectedRecord.getHrid());
    assertEquals(INSTANCE, affectedRecord.getRecordType());
    assertTrue(Objects.isNull(affectedRecord.getInventoryRecordLink()));
    verify(configurationsClient, never())
      .getConfigsFromModConfigByQuery(anyString(), anyString(), any(OkapiConnectionParams.class));
    assertTrue(affectedRecord.getAffectedRecords().isEmpty());
  }

  @Test
  void buildAffectedRecord_shouldBuildRecordWithTypeAndId_whenGivenJsonIsEmpty() {

    // when
    AffectedRecord affectedRecord = affectedRecordBuilder.build(new JsonObject(), "jobId", INSTANCE_ID, false, params);

    // then
    assertNull(affectedRecord.getTitle());
    assertNull(affectedRecord.getHrid());
    assertEquals(INSTANCE_ID, affectedRecord.getId());
    assertEquals(INSTANCE, affectedRecord.getRecordType());
    assertNull(affectedRecord.getInventoryRecordLink());
    verify(configurationsClient, never())
      .getConfigsFromModConfigByQuery(anyString(), anyString(), any(OkapiConnectionParams.class));
    assertTrue(affectedRecord.getAffectedRecords().isEmpty());
  }
}
