package org.folio.service.mapping;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.folio.TestUtil;
import org.folio.clients.ConfigurationsClient;
import org.folio.processor.ReferenceData;
import org.folio.processor.rule.Rule;
import org.folio.rest.jaxrs.model.*;
import org.folio.service.mapping.referencedata.ReferenceDataImpl;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.ReferenceDataResponseUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.marc4j.marc.VariableField;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.folio.TestUtil.CALLNUMBER_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_FIELD_PATH;
import static org.folio.TestUtil.CALLNUMBER_PREFIX_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_PREFIX_FIELD_PATH;
import static org.folio.TestUtil.CALLNUMBER_SUFFIX_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_SUFFIX_FIELD_PATH;
import static org.folio.TestUtil.EFFECTIVECALLNUMBER_CALL_NUMBER_FIELD_ID;
import static org.folio.TestUtil.EFFECTIVE_LOCATION_FIELD_ID;
import static org.folio.TestUtil.EFFECTIVE_LOCATION_PATH;
import static org.folio.TestUtil.ELECTRONIC_ACCESS_LINKTEXT_FIELD_ID;
import static org.folio.TestUtil.ELECTRONIC_ACCESS_URI_FIELD_ID;
import static org.folio.TestUtil.HOLDINGS_ELECTRONIC_ACCESS_LINK_TEXT_PATH;
import static org.folio.TestUtil.HOLDINGS_ELECTRONIC_ACCESS_URI_PATH;
import static org.folio.TestUtil.ITEMS_EFFECTIVE_CALL_NUMBER_PATH;
import static org.folio.TestUtil.ITEMS_ELECTRONIC_ACCESS_LINK_TEXT_PATH;
import static org.folio.TestUtil.ITEMS_ELECTRONIC_ACCESS_URI_PATH;
import static org.folio.TestUtil.MATERIALTYPE_FIELD_ID;
import static org.folio.TestUtil.MATERIAL_TYPE_ID_PATH;
import static org.folio.TestUtil.PERMANENT_LOCATION_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_PATH;
import static org.folio.TestUtil.TEMPORARY_LOCATION_FIELD_ID;
import static org.folio.TestUtil.TEMPORARY_LOCATION_PATH;
import static org.folio.TestUtil.getFileFromResources;
import static org.folio.TestUtil.readFileContentFromResources;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.RecordType.ITEM;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.CONTRIBUTOR_NAME_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_FORMATS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.LOCATIONS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MATERIAL_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.NATURE_OF_CONTENT_TERMS;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class MappingServiceUnitTest {

  @InjectMocks
  private MappingServiceImpl mappingService;
  @Mock
  private ConfigurationsClient configurationsClient;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  private String jobExecutionId = "67429e0e-601a-423b-9a29-dec4a30c8534";
  private OkapiConnectionParams params = new OkapiConnectionParams();
  private ReferenceData referenceData = new ReferenceDataImpl();

  MappingServiceUnitTest() {
    referenceData.put(NATURE_OF_CONTENT_TERMS, ReferenceDataResponseUtil.getNatureOfContentTerms());
    referenceData.put(IDENTIFIER_TYPES, ReferenceDataResponseUtil.getIdentifierTypes());
    referenceData.put(CONTRIBUTOR_NAME_TYPES, ReferenceDataResponseUtil.getContributorNameTypes());
    referenceData.put(LOCATIONS, ReferenceDataResponseUtil.getLocations());
    referenceData.put(MATERIAL_TYPES, ReferenceDataResponseUtil.getMaterialTypes());
    referenceData.put(INSTANCE_TYPES, ReferenceDataResponseUtil.getInstanceTypes());
    referenceData.put(INSTANCE_FORMATS, ReferenceDataResponseUtil.getInstanceFormats());
    referenceData.put(ELECTRONIC_ACCESS_RELATIONSHIPS, ReferenceDataResponseUtil.getElectronicAccessRelationships());
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
    Mockito.when(configurationsClient.getRulesFromConfiguration(any(OkapiConnectionParams.class)))
      .thenReturn(Collections.emptyList());
    // when
    List<String> actualMarcRecords = mappingService.map(instances, new MappingProfile(), jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);
    File expectedJsonRecords = getFileFromResources("mapping/expected_marc.json");
    String expectedMarcRecord = TestUtil.getExpectedMarcFromJson(expectedJsonRecords);
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
    Mockito.when(configurationsClient.getRulesFromConfiguration(any(OkapiConnectionParams.class)))
      .thenReturn(Collections.emptyList());
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_record_with_holdings_and_items.json");
    String expectedMarcRecord = TestUtil.getExpectedMarcFromJson(expectedJsonRecords);
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
    //all transformations provided in the mapping profile must be mapped
    Assert.assertEquals(11, appendedMarcRecords.stream().map(vf -> vf.getTag()).collect(Collectors.toSet()).size());
    Assert.assertEquals(20, appendedMarcRecords.size());
  }

  @Test
  void shouldMapInstanceHoldingsAndItem_to_marcRecord_whenMappingProfileTransformationsAreNotEmptyAndRulesFromModConfig() throws IOException {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createHoldingsAndItemSimpleFieldTransformations());
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    Mockito.when(configurationsClient.getRulesFromConfiguration(any(OkapiConnectionParams.class)))
      .thenReturn(getDefaultRules());
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_record_with_holdings_and_items.json");
    String expectedMarcRecord = TestUtil.getExpectedMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  /**
   * This test makes sure if the path specified in transformation Fields, is correct and parsable,
   * by creating the mapping profile from the transformation fields
   */
  @Test
  void shouldMapInstances_to_marcRecord_withMappingProfileFromTransformationFields() throws FileNotFoundException {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createInstanceTransformationsFromTransformationFields());
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    Mockito.when(configurationsClient.getRulesFromConfiguration(any(OkapiConnectionParams.class)))
      .thenReturn(Collections.emptyList());
    System.out.println(mappingProfile.getTransformations());
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);

    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_instance_transformationFields.json");
    String expectedMarcRecord = TestUtil.getExpectedMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  private List<Transformations> createHoldingsAndItemSimpleFieldTransformations() {
    List<Transformations> transformations = new ArrayList<>();
    transformations.add(createTransformations(CALLNUMBER_PREFIX_FIELD_ID, CALLNUMBER_PREFIX_FIELD_PATH, "900ff$b", HOLDINGS));
    transformations.add(createTransformations(CALLNUMBER_FIELD_ID, CALLNUMBER_FIELD_PATH, "900ff$a", HOLDINGS));
    transformations.add(createTransformations(CALLNUMBER_SUFFIX_FIELD_ID, CALLNUMBER_SUFFIX_FIELD_PATH, "902  $a", HOLDINGS));
    transformations.add(createTransformations(ELECTRONIC_ACCESS_LINKTEXT_FIELD_ID, HOLDINGS_ELECTRONIC_ACCESS_LINK_TEXT_PATH, "903  $a", HOLDINGS));
    transformations.add(createTransformations(ELECTRONIC_ACCESS_URI_FIELD_ID, HOLDINGS_ELECTRONIC_ACCESS_URI_PATH, "90412$a", HOLDINGS));
    transformations.add(createTransformations(PERMANENT_LOCATION_FIELD_ID, PERMANENT_LOCATION_PATH, "905  $a", HOLDINGS));
    transformations.add(createTransformations(TEMPORARY_LOCATION_FIELD_ID, TEMPORARY_LOCATION_PATH, "906  $b", HOLDINGS));
    transformations.add(createTransformations(EFFECTIVECALLNUMBER_CALL_NUMBER_FIELD_ID, ITEMS_EFFECTIVE_CALL_NUMBER_PATH, "907  $a", ITEM));
    transformations.add(createTransformations(ELECTRONIC_ACCESS_LINKTEXT_FIELD_ID, ITEMS_ELECTRONIC_ACCESS_LINK_TEXT_PATH, "908  $a", ITEM));
    transformations.add(createTransformations(ELECTRONIC_ACCESS_URI_FIELD_ID, ITEMS_ELECTRONIC_ACCESS_URI_PATH, "9091 $a", ITEM));
    transformations.add(createTransformations(MATERIALTYPE_FIELD_ID, MATERIAL_TYPE_ID_PATH, "910  $a", ITEM));
    transformations.add(createTransformations(EFFECTIVE_LOCATION_FIELD_ID, EFFECTIVE_LOCATION_PATH, "911  $a", ITEM));
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
   * Construct the mapping profile Transformations from the fields from the Transformation Fields
   * (that are usually accessed on the UI via an API)
   *
   * @return
   */
  private List<Transformations> createInstanceTransformationsFromTransformationFields() {
    List<Transformations> transformations = new ArrayList<>();
    AtomicInteger tag = new AtomicInteger(899);
    TransformationFieldCollection transformationFields = new JsonObject(
        readFileContentFromResources("mapping/expectedTransformationFields.json")).mapTo(TransformationFieldCollection.class);
    transformationFields.getTransformationFields().stream()
      .filter(tfn -> tfn.getRecordType().equals(TransformationField.RecordType.INSTANCE))
      .forEach(tfn -> {
        String idx = tag.incrementAndGet() + "ff$a";
        transformations.add(createTransformations(tfn.getFieldId(), tfn.getPath(), idx, RecordType.fromValue(tfn.getRecordType()
          .toString())));
      });

    return transformations;
  }

  private List<Rule> getDefaultRules() throws IOException {
    URL url = Resources.getResource("rules/rulesDefault.json");
    String stringRules = Resources.toString(url, StandardCharsets.UTF_8);
    return Lists.newArrayList(Json.decodeValue(stringRules, Rule[].class));
  }

}

