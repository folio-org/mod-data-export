package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.folio.TestUtil.getResourceAsString;

@RunWith(MockitoJUnitRunner.class)
public class MappingServiceUnitTest {

  @Test
  public void shouldNotThrowAnyException() {
    List<JsonObject> givenInstances = Collections.emptyList();
    MappingService mappingService = new MappingServiceImpl();
    assertThatCode(() -> mappingService.map(givenInstances)).doesNotThrowAnyException();
  }

  @Test
  public void shouldMapInstanceToMarcRecord() {
    // given
    MappingService mappingService = new MappingServiceImpl();
    JsonObject instance = new JsonObject(getResourceAsString("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    // when
    List<String> actualMarcRecords = mappingService.map(instances);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);
    String expectedMarcRecord = getResourceAsString("mapping/expected_marc_record.mrc");
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }
}

