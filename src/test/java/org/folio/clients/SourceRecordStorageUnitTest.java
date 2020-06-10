package org.folio.clients;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.RestVerticleTestBase;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

@RunWith(VertxUnitRunner.class)
class SourceRecordStorageUnitTest extends RestVerticleTestBase {
  private static final int LIMIT = 20;
  private static OkapiConnectionParams okapiConnectionParams;

  @BeforeAll
  public static void beforeClass() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void shouldReturnExistingMarcRecords() {
    // given
    SourceRecordStorageClient srsClient = new SourceRecordStorageClient();
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    // when
    Optional<JsonObject> srsResponse = srsClient.getRecordsByIds(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(srsResponse.isPresent());
    Assert.assertEquals(2, srsResponse.get().getJsonArray("records").getList().size());
  }
}
