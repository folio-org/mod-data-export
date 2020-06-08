package org.folio.service.mapping;

import static org.folio.TestUtil.getFileFromResources;
import static org.folio.TestUtil.readFileContentFromResources;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.RecordType.ITEM;
import static org.mockito.ArgumentMatchers.any;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.*;
import java.util.*;
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
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

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
    referenceData.addContributorNameTypes(getContributorNameTypes());
    referenceData.addLocations(getLocations());
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

  private Map<String, JsonObject> getLocations() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_locations_response.json"))
        .getJsonArray("locations");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : identifierTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  private Map<String, JsonObject> getContributorNameTypes() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_contributor_name_types_response.json"))
        .getJsonArray("contributorNameTypes");
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
  void shouldMapInstance_to_marcRecord_whenMappingProfileTransformationsEmpty() throws FileNotFoundException {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    // when
    List<String> actualMarcRecords = mappingService.map(instances, new MappingProfile(), jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);
    File expectedJsonRecords = getFileFromResources("mapping/expected_marc.json");
    String expectedMarcRecord = getExpectedMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);

  }


  @Test
  void shouldMapInstanceHoldingsAndItem_to_marcRecord_whenMappingProfileTransformationsAreNotEmpty() throws FileNotFoundException {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createHoldingsAndItemSimpleFieldTransformations());
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_record_with_holdings_and_items.json");
    String expectedMarcRecord = getExpectedMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  @Test
  void shouldReturnVariableFieldsForHoldingsAndItem_whenMappingProfileTransformationsAreProvided() throws FileNotFoundException {
    // given
    JsonObject srsRecord = new JsonObject(readFileContentFromResources("mapping/given_HoldingsItems.json"));
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createHoldingsAndItemSimpleFieldTransformations());
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    // when
    List<VariableField> appendedMarcRecords = mappingService.mapFields(srsRecord, mappingProfile, jobExecutionId, params);
    // then
    System.out.print(appendedMarcRecords);
    Assert.assertEquals(19, appendedMarcRecords.size());
  }

  private List<Transformations> createHoldingsAndItemSimpleFieldTransformations() {
    List<Transformations> transformations = new ArrayList<>();
    transformations.add(createTransformations("callNumber", "$.holdings[*].callNumber", "900ff$a", HOLDINGS));
    transformations.add(createTransformations("callNumberPrefix", "$.holdings[*].callNumberPrefix", "901  $a", HOLDINGS));
    transformations.add(createTransformations("callNumberSuffix", "$.holdings[*].callNumberSuffix", "902  $a", HOLDINGS));
    transformations.add(createTransformations("electronicAccess.linkText", "$.holdings[*].electronicAccess[*].linkText", "903  $a", HOLDINGS));
    transformations.add(createTransformations("electronicAccess.uri", "$.holdings[*].electronicAccess[*].uri", "90412$a", HOLDINGS));
    transformations.add(createTransformations("permanentLocationId", "$.holdings[*].permanentLocationId", "905  $a", HOLDINGS));
    transformations.add(createTransformations("temporaryLocationId", "$.holdings[*].temporaryLocationId", "906  $b", HOLDINGS));
    transformations.add(createTransformations("effectiveCallNumberComponents.callNumber", "$.items[*].effectiveCallNumberComponents.callNumber", "907  $a", ITEM));
    transformations.add(createTransformations("electronicAccess.linkText", "$.items[*].electronicAccess[*].linkText", "908  $a", ITEM));
    transformations.add(createTransformations("electronicAccess.uri", "$.items[*].electronicAccess[*].uri", "9091 $a", ITEM));
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

  /**
   * This method fetches the expected value from Json file and converts it into MARC
   *
   * @return expected Json converted to marc format
   * @throws FileNotFoundException
   */
  private String getExpectedMarcFromJson(File expectedFile) throws FileNotFoundException {
    InputStream inputStream = new FileInputStream(expectedFile);
    MarcReader marcReader = new MarcJsonReader(inputStream);
    OutputStream outputStream = new ByteArrayOutputStream();
    MarcWriter writer = new MarcStreamWriter(outputStream);
    while (marcReader.hasNext()) {
      Record record = marcReader.next();
      writer.write(record);
    }

    writer.close();
    return outputStream.toString();
  }

}

