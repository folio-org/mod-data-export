package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.clients.SourceRecordStorageClient;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

@RunWith(VertxUnitRunner.class)
class SourceRecordStorageTest extends RestVerticleTestBase {
  private static OkapiConnectionParams okapiConnectionParams;

  @BeforeAll
  public static void beforeClass() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void shouldReturnExistingMarcRecordsForProvidedInstanceUUIDs() {
    // given
    SourceRecordStorageClient srsClient = new SourceRecordStorageClient();
    List<String> uuids = Arrays.asList("ae573875-fbc8-40e7-bda7-0ac283354226", "5fc04e92-70dd-46b8-97ea-194015762a60");
    // when
    Optional<JsonObject> srsResponse = srsClient.getRecordsByIds(uuids, "instance", UUID.randomUUID().toString(), okapiConnectionParams);
    // then
    Assert.assertTrue(srsResponse.isPresent());
    Assert.assertEquals(2, srsResponse.get().getJsonArray("sourceRecords").getList().size());
  }

  @Test
  void shouldReturnExistingMarcRecordsForProvidedHoldingUUIDs() {
    // given
    SourceRecordStorageClient srsClient = new SourceRecordStorageClient();
    List<String> uuids = Arrays.asList("49713f91-2446-467c-a75c-f8cbbe38985f", "9701533f-5a1f-45ce-8bd3-0a9666159e2f");
    // when
    Optional<JsonObject> srsResponse = srsClient.getRecordsByIds(uuids, "holding", UUID.randomUUID().toString(), okapiConnectionParams);
    // then
    Assert.assertTrue(srsResponse.isPresent());
    Assert.assertEquals(2, srsResponse.get().getJsonArray("sourceRecords").getList().size());
  }

}
