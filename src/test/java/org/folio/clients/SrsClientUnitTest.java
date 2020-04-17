package org.folio.clients;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.HttpServerTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(VertxUnitRunner.class)
public class SrsClientUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;

  @Spy
  StorageClient client;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldReturnExistingMarcRecords() throws IOException {
    // given
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    // when
    Optional<JsonObject> srsResponce = client.getByIdsFromSRS(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(srsResponce.isPresent());
    Assert.assertEquals(2, srsResponce.get().getJsonArray("records").getList().size());
  }
}
