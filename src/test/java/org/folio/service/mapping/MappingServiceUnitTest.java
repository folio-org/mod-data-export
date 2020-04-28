package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.settings.Settings;
import org.folio.service.mapping.settings.MappingSettingsProvider;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.folio.TestUtil.getResourceAsString;

@RunWith(MockitoJUnitRunner.class)
public class MappingServiceUnitTest {

  @Mock
  private MappingSettingsProvider mappingSettingsProvider;
  private String jobExecutionId = "67429e0e-601a-423b-9a29-dec4a30c8534";
  private OkapiConnectionParams params = new OkapiConnectionParams();

  @Before
  public void mockSettings() {
    Settings settings = new Settings();
    JsonObject natureOfContentTerm_audioBook = new JsonObject()
      .put("id", "96879b60-098b-453b-bf9a-c47866f1ab2a")
      .put("name", "audiobook")
      .put("source", "folio");
    JsonObject natureOfContentTerm_autobiography = new JsonObject()
      .put("id", "04a6a8d2-f902-4774-b15f-d8bd885dc804")
      .put("name", "autobiography")
      .put("source", "folio");
    settings.addNatureOfContentTerms(Arrays.asList(natureOfContentTerm_audioBook, natureOfContentTerm_autobiography));
    Mockito.when(mappingSettingsProvider.getSettings(jobExecutionId, params)).thenReturn(settings);
  }

  @Test
  public void shouldNotThrowAnyException() {
    List<JsonObject> givenInstances = Collections.emptyList();
    MappingService mappingService = new MappingServiceImpl(mappingSettingsProvider);
    assertThatCode(() -> mappingService.map(givenInstances, jobExecutionId, params)).doesNotThrowAnyException();
  }

  @Test
  public void shouldMapInstanceToMarcRecord() {
    // given
    MappingService mappingService = new MappingServiceImpl(mappingSettingsProvider);
    JsonObject instance = new JsonObject(getResourceAsString("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    // when
    List<String> actualMarcRecords = mappingService.map(instances, jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);
    String expectedMarcRecord = getResourceAsString("mapping/expected_marc_record.mrc");
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }
}

