package org.folio.service.mapping;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.TestUtil;
import org.folio.service.mapping.settings.MappingSettingsProvider;
import org.folio.service.mapping.settings.Settings;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.TestUtil.getResourceAsString;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class MappingServiceUnitTest {

  @Mock
  private MappingSettingsProvider mappingSettingsProvider;
  private String jobExecutionId = "67429e0e-601a-423b-9a29-dec4a30c8534";
  private OkapiConnectionParams params = new OkapiConnectionParams();

  @Before
  public void mockSettings() {
    Settings settings = new Settings();
    settings.addNatureOfContentTerms(getNatureOfContentTerms());
    Mockito.when(mappingSettingsProvider.getSettings(jobExecutionId, params)).thenReturn(settings);
  }

  private Map<String, JsonObject> getNatureOfContentTerms() {
    JsonArray natureOfContentTermArray =
      new JsonObject(TestUtil.getResourceAsString("mockData/settings/get_nature_of_content_terms_response.json"))
        .getJsonArray("natureOfContentTerms");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : natureOfContentTermArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  @Test
  public void shouldReturnEmptyRecords_for_emptyInstances() {
    // given
    MappingService mappingService = new MappingServiceImpl(mappingSettingsProvider);
    List<JsonObject> givenInstances = Collections.emptyList();
    // when
    List<String> actualRecords = mappingService.map(givenInstances, jobExecutionId, params);
    // then
    Assert.assertNotNull(actualRecords);
    Assert.assertEquals(actualRecords.size(), 0);
    Mockito.verify(mappingSettingsProvider, Mockito.never()).getSettings(any(String.class), any(OkapiConnectionParams.class));
  }

  @Test
  public void shouldMapInstance_to_marcRecord() {
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

