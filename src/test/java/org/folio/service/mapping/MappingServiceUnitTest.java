package org.folio.service.mapping;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.referencedata.ReferenceData;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.TestUtil.readFileContentFromResources;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.RecordType.ITEM;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class MappingServiceUnitTest {

  @InjectMocks
  private MappingServiceImpl mappingService;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  private String jobExecutionId = "67429e0e-601a-423b-9a29-dec4a30c8534";
  private OkapiConnectionParams params = new OkapiConnectionParams();
  private ReferenceData referenceData = new ReferenceData();

  MappingServiceUnitTest() {
    referenceData.addNatureOfContentTerms(getNatureOfContentTerms());
    referenceData.addIdentifierTypes(getIdentifierTypes());
  }

  private Map<String, JsonObject> getNatureOfContentTerms() {
    JsonArray natureOfContentTermArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_nature_of_content_terms_response.json"))
        .getJsonArray("natureOfContentTerms");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : natureOfContentTermArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  private Map<String, JsonObject> getIdentifierTypes() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_identifier_types_response.json"))
        .getJsonArray("identifierTypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : identifierTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  @Test
  void shouldReturnEmptyRecords_for_emptyInstances() {
    // given
    List<JsonObject> givenInstances = Collections.emptyList();
    // when
    List<String> actualRecords = mappingService.map(givenInstances, new MappingProfile(), jobExecutionId, params);
    // then
    Assert.assertNotNull(actualRecords);
    Assert.assertEquals(0, actualRecords.size());
    Mockito.verify(referenceDataProvider, Mockito.never()).get(any(String.class), any(OkapiConnectionParams.class));
  }

  @Test
  void shouldMapInstance_to_marcRecord_whenMappingProfileTransformationsEmpty() {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    Mockito.when(referenceDataProvider.get(jobExecutionId, params)).thenReturn(referenceData);
    // when
    List<String> actualMarcRecords = mappingService.map(instances, new MappingProfile(), jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);
    String expectedMarcRecord = readFileContentFromResources("mapping/expected_marc_record.mrc");
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  @Test
  void shouldMapInstanceHoldingsAndItem_to_marcRecord_whenMappingProfileTransformationsAreNotEmpty() {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createHoldingsAndItemSimpleFieldTransformations());
    Mockito.when(referenceDataProvider.get(jobExecutionId, params)).thenReturn(referenceData);
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);
    String expectedMarcRecord = readFileContentFromResources("mapping/expected_marc_record_with_holdings_and_items.mrc");
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  private List<Transformations> createHoldingsAndItemSimpleFieldTransformations() {
    List<Transformations> transformations = new ArrayList<>();
    transformations.add(createTransformations("callNumber", "$.holdings[*].callNumber", "900 $a", HOLDINGS));
    transformations.add(createTransformations("callNumberPrefix", "$.holdings[*].callNumberPrefix", "901 $a", HOLDINGS));
    transformations.add(createTransformations("callNumberSuffix", "$.holdings[*].callNumberSuffix", "902 $a", HOLDINGS));
    transformations.add(createTransformations("electronicAccess.linkText", "$.holdings[*].electronicAccess[*].linkText", "903 $a", HOLDINGS));
    transformations.add(createTransformations("electronicAccess.uri", "$.holdings[*].electronicAccess[*].uri", "904 $a", HOLDINGS));
    transformations.add(createTransformations("effectiveCallNumberComponents.callNumber", "$.items[*].effectiveCallNumberComponents.callNumber", "905 $a", ITEM));
    transformations.add(createTransformations("electronicAccess.linkText", "$.items[*].electronicAccess[*].linkText", "906 $a", ITEM));
    transformations.add(createTransformations("electronicAccess.uri", "$.items[*].electronicAccess[*].uri", "907 $a", ITEM));
    return transformations;
  }

  private Transformations createTransformations(String fieldId, String fieldPath, String value, RecordType recordType) {
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setFieldId(fieldId);
    transformations.setPath(fieldPath);
    transformations.setTransformation(value);
    transformations.setRecordType(recordType);
    return transformations;
  }
}

