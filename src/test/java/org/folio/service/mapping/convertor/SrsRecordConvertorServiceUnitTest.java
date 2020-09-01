package org.folio.service.mapping.convertor;

import static org.folio.TestUtil.readFileContentFromResources;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.RecordType.ITEM;
import static org.junit.Assert.assertEquals;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.CONTRIBUTOR_NAME_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.LOCATIONS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.NATURE_OF_CONTENT_TERMS;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;
import org.folio.clients.ConfigurationsClient;
import org.folio.processor.ReferenceData;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.mapping.MappingService;
import org.folio.service.mapping.MappingServiceImpl;
import org.folio.service.mapping.referencedata.ReferenceDataImpl;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.ReferenceDataResponseUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class SrsRecordConvertorServiceUnitTest {

  @InjectMocks
  SrsRecordConvertorService srsRecordConvertorService;
  @InjectMocks
  private MappingService mappingService = Mockito.spy(new MappingServiceImpl());

  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  @Mock
  private ConfigurationsClient configurationsClient;
  private String jobExecutionId = "67429e0e-601a-423b-9a29-dec4a30c8534";
  private OkapiConnectionParams params = new OkapiConnectionParams();
  private ReferenceData referenceData = new ReferenceDataImpl();

  SrsRecordConvertorServiceUnitTest() {
    referenceData.put(NATURE_OF_CONTENT_TERMS, ReferenceDataResponseUtil.getNatureOfContentTerms());
    referenceData.put(IDENTIFIER_TYPES, ReferenceDataResponseUtil.getIdentifierTypes());
    referenceData.put(CONTRIBUTOR_NAME_TYPES, ReferenceDataResponseUtil.getContributorNameTypes());
    referenceData.put(LOCATIONS, ReferenceDataResponseUtil.getLocations());
  }

  @Test
  void shouldNotTransformSRSRecords_for_MappingProfileWithNoTransformations() {
    MappingProfile mappingProfile = new MappingProfile();

    //given
    JsonArray srsRecords =
        new JsonObject(readFileContentFromResources("mockData/srs/get_records_response.json"))
          .getJsonArray("sourceRecords");
    JsonObject srsRecord = srsRecords.getJsonObject(0);
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
    .thenReturn(referenceData);

    JsonArray holdingRecords =
        new JsonObject(readFileContentFromResources("mockData/inventory/holdings_in00041.json"))
          .getJsonArray("holdingsRecords");
    List<JsonObject> result = new ArrayList<>();
    for (Object holding : holdingRecords) {
      result.add(JsonObject.mapFrom(holding));
    }

    //when
    List<String> afterConversion = srsRecordConvertorService.transformSrsRecord(mappingProfile, Arrays.asList(srsRecord), jobExecutionId, params);
    JsonObject afterJson = new JsonObject(afterConversion.get(0));

    //Then
    //As no transfomation is applied both should have same fields and leader
    assertEquals(srsRecord.getJsonObject("parsedRecord").getJsonObject("content"), afterJson);
  }

  @Test
  void shouldTransformSRSRecords_for_CustomMappingProfile() {
    //given
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(Arrays.asList(RecordType.HOLDINGS));
    mappingProfile.setTransformations(createHoldingsAndItemSimpleFieldTransformations());

    JsonArray srsRecords =
        new JsonObject(readFileContentFromResources("mockData/srs/get_records_response.json"))
          .getJsonArray("sourceRecords");
    JsonObject srsRecord = srsRecords.getJsonObject(0);
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
    .thenReturn(referenceData);

    JsonArray holdingRecords =
        new JsonObject(readFileContentFromResources("mockData/inventory/holdings_in00041.json"))
          .getJsonArray("holdingsRecords");
    List<JsonObject> result = new ArrayList<>();
    for (Object holding : holdingRecords) {
      result.add(JsonObject.mapFrom(holding));
    }
    //when
    Mockito.when(recordLoaderService.getHoldingsForInstance("ae573875-fbc8-40e7-bda7-0ac283354226", params))
    .thenReturn(result);
    List<String> afterConversion = srsRecordConvertorService.transformSrsRecord(mappingProfile, Arrays.asList(srsRecord), jobExecutionId, params);
    JsonObject afterJson = new JsonObject(afterConversion.get(0));

    //then
    //Holdings are fetched, and not items
    Mockito.verify(recordLoaderService, Mockito.times(1)).getHoldingsForInstance(anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(0)).getAllItemsForHolding(anyList(), any(OkapiConnectionParams.class));
    //New transfomations must be applied
    assertNotEquals(srsRecord.getJsonObject("parsedRecord").getJsonObject("content"), afterJson);

  }


  @Test
  void shouldTransformSRSRecords_for_CustomMappingProfileWithHoldingsItems() {
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(Arrays.asList(RecordType.HOLDINGS, RecordType.ITEM));
    mappingProfile.setTransformations(createHoldingsAndItemSimpleFieldTransformations());

    JsonArray srsRecords =
        new JsonObject(readFileContentFromResources("mockData/srs/get_records_response.json"))
          .getJsonArray("sourceRecords");
    JsonObject srsRecord = srsRecords.getJsonObject(0);
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
    .thenReturn(referenceData);

    JsonArray holdingRecords =
        new JsonObject(readFileContentFromResources("mockData/inventory/holdings_in00041.json"))
          .getJsonArray("holdingsRecords");
    List<JsonObject> result = new ArrayList<>();
    for (Object holding : holdingRecords) {
      result.add(JsonObject.mapFrom(holding));
    }
    Mockito.when(recordLoaderService.getHoldingsForInstance("ae573875-fbc8-40e7-bda7-0ac283354226", params))
    .thenReturn(result);
    List<String> afterConversion = srsRecordConvertorService.transformSrsRecord(mappingProfile, Arrays.asList(srsRecord), jobExecutionId, params);
    JsonObject afterJson = new JsonObject(afterConversion.get(0));
    assertNotEquals(srsRecord.getJsonObject("parsedRecord").getJsonObject("content"), afterJson);
    Mockito.verify(recordLoaderService, Mockito.times(1)).getHoldingsForInstance(anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(1)).getAllItemsForHolding(anyList(), any(OkapiConnectionParams.class));
  }


  private List<Transformations> createHoldingsAndItemSimpleFieldTransformations() {
    List<Transformations> transformations = new ArrayList<>();
    transformations.add(createTransformations("callNumber", "$.holdings[*].callNumber", "900ff$a", HOLDINGS));
    transformations.add(createTransformations("callNumberPrefix", "$.holdings[*].callNumberPrefix", "901  $a", HOLDINGS));
    transformations.add(createTransformations("callNumberSuffix", "$.holdings[*].callNumberSuffix", "902  $a", HOLDINGS));
    transformations.add(createTransformations("electronicAccess.linkText", "$.holdings[*].electronicAccess[*].linkText", "903  $a", HOLDINGS));
    transformations.add(createTransformations("electronicAccess.uri", "$.holdings[*].electronicAccess[*].uri", "90412$a", HOLDINGS));
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

}

