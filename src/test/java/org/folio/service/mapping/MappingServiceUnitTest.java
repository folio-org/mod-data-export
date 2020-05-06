package org.folio.service.mapping;

import com.google.common.base.Splitter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.TestUtil;
import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.processor.RuleFactory;
import org.folio.service.mapping.profiles.RecordType;
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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.folio.TestUtil.getResourceAsString;

@RunWith(MockitoJUnitRunner.class)
public class MappingServiceUnitTest {

  @Mock
  private MappingSettingsProvider mappingSettingsProvider;
  private RuleProcessor ruleProcessor;
  private String jobExecutionId = "67429e0e-601a-423b-9a29-dec4a30c8534";
  private OkapiConnectionParams params = new OkapiConnectionParams();
  private RuleFactory ruleProcessorFactory = new RuleFactory();

  @Before
  public void mockSettings() throws IOException {
    Settings settings = new Settings();
    settings.addNatureOfContentTerms(getNatureOfContentTerms());
    Mockito.when(mappingSettingsProvider.getSettings(jobExecutionId, params)).thenReturn(settings);
    ruleProcessor = ruleProcessorFactory.createDefault();
  }

  private Map<String, JsonObject> getNatureOfContentTerms() {
    JsonArray natureOfContentTermArray =
      new JsonObject(TestUtil.getResourceAsString("clients/inventory/get_nature_of_content_terms_response.json"))
        .getJsonArray("natureOfContentTerms");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : natureOfContentTermArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  @Test
  public void shouldNotThrowAnyException() {
    List<JsonObject> givenInstances = Collections.emptyList();
    MappingService mappingService = new MappingServiceImpl(mappingSettingsProvider, ruleProcessor);
    assertThatCode(() -> mappingService.map(givenInstances, jobExecutionId, params)).doesNotThrowAnyException();
  }

  @Test
  public void shouldMapInstanceToMarcRecord() {
    // given
    MappingService mappingService = new MappingServiceImpl(mappingSettingsProvider, ruleProcessor);
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

  @Test
  public void test() {
    List<String> field = Splitter.on(".").omitEmptyStrings().splitToList("field.subfield");
    System.out.println(field);

    System.out.println(RecordType.ITEM.toString().toLowerCase());
  }
}

