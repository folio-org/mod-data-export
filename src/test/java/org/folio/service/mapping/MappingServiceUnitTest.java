package org.folio.service.mapping;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.folio.TestUtil;
import org.folio.clients.ConfigurationsClient;
import org.folio.processor.ReferenceData;
import org.folio.processor.rule.Rule;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.rest.jaxrs.model.Transformations;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.TestUtil.CALLNUMBER_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_FIELD_PATH;
import static org.folio.TestUtil.CALLNUMBER_PREFIX_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_PREFIX_FIELD_PATH;
import static org.folio.TestUtil.CALLNUMBER_SUFFIX_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_SUFFIX_FIELD_PATH;
import static org.folio.TestUtil.EFFECTIVECALLNUMBER_CALL_NUMBER_FIELD_ID;
import static org.folio.TestUtil.ELECTRONIC_ACCESS_LINKTEXT_FIELD_ID;
import static org.folio.TestUtil.ELECTRONIC_ACCESS_URI_FIELD_ID;
import static org.folio.TestUtil.HOLDINGS_ELECTRONIC_ACCESS_LINK_TEXT_PATH;
import static org.folio.TestUtil.HOLDINGS_ELECTRONIC_ACCESS_URI_PATH;
import static org.folio.TestUtil.INSTANCE_ELECTRONIC_ACCESS_LINK_TEXT_FIELD_ID;
import static org.folio.TestUtil.INSTANCE_ELECTRONIC_ACCESS_LINK_TEXT_PATH;
import static org.folio.TestUtil.INSTANCE_ELECTRONIC_ACCESS_URI_FIELD_ID;
import static org.folio.TestUtil.INSTANCE_ELECTRONIC_ACCESS_URI_FIELD_PATH;
import static org.folio.TestUtil.INSTANCE_HR_ID_FIELD_ID;
import static org.folio.TestUtil.INSTANCE_HR_ID_FIELD_PATH;
import static org.folio.TestUtil.INSTANCE_METADATA_CREATED_DATE_FIELD_ID;
import static org.folio.TestUtil.INSTANCE_METADATA_CREATED_DATE_FIELD_PATH;
import static org.folio.TestUtil.INSTANCE_METADATA_UPDATED_DATE_FIELD_ID;
import static org.folio.TestUtil.INSTANCE_METADATA_UPDATED_DATE_FIELD_PATH;
import static org.folio.TestUtil.ITEMS_EFFECTIVE_CALL_NUMBER_PATH;
import static org.folio.TestUtil.ITEMS_ELECTRONIC_ACCESS_LINK_TEXT_PATH;
import static org.folio.TestUtil.ITEMS_ELECTRONIC_ACCESS_URI_PATH;
import static org.folio.TestUtil.MATERIALTYPE_FIELD_ID;
import static org.folio.TestUtil.MATERIAL_TYPE_ID_PATH;
import static org.folio.TestUtil.getFileFromResources;
import static org.folio.TestUtil.readFileContentFromResources;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.RecordType.ITEM;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MATERIAL_TYPES;
import static org.folio.util.ExternalPathResolver.CALL_NUMBER_TYPES;
import static org.folio.util.ExternalPathResolver.CAMPUSES;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE_FORMATS;
import static org.folio.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.util.ExternalPathResolver.INSTITUTIONS;
import static org.folio.util.ExternalPathResolver.LIBRARIES;
import static org.folio.util.ExternalPathResolver.LOAN_TYPES;
import static org.folio.util.ExternalPathResolver.LOCATIONS;
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
    referenceData.put(CONTENT_TERMS, ReferenceDataResponseUtil.getNatureOfContentTerms());
    referenceData.put(IDENTIFIER_TYPES, ReferenceDataResponseUtil.getIdentifierTypes());
    referenceData.put(CONTRIBUTOR_NAME_TYPES, ReferenceDataResponseUtil.getContributorNameTypes());
    referenceData.put(CALL_NUMBER_TYPES, ReferenceDataResponseUtil.getCallNumberTypes());
    referenceData.put(LOCATIONS, ReferenceDataResponseUtil.getLocations());
    referenceData.put(MATERIAL_TYPES, ReferenceDataResponseUtil.getMaterialTypes());
    referenceData.put(INSTANCE_TYPES, ReferenceDataResponseUtil.getInstanceTypes());
    referenceData.put(LOAN_TYPES, ReferenceDataResponseUtil.getLoanTypes());
    referenceData.put(INSTANCE_FORMATS, ReferenceDataResponseUtil.getInstanceFormats());
    referenceData.put(ELECTRONIC_ACCESS_RELATIONSHIPS, ReferenceDataResponseUtil.getElectronicAccessRelationships());
    referenceData.put(LIBRARIES, ReferenceDataResponseUtil.getLibraries());
    referenceData.put(CAMPUSES, ReferenceDataResponseUtil.getCampuses());
    referenceData.put(INSTITUTIONS, ReferenceDataResponseUtil.getInstitutions());
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
    String expectedMarcRecord = TestUtil.getMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);

  }

  @Test
  void shouldMapInstanceHoldingsAndItem_to_marcRecord_whenMappingProfileTransformationsAreNotEmpty() throws FileNotFoundException {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createHoldingsAndItemSimpleFieldTransformations());
    mappingProfile.setRecordTypes(Collections.singletonList(INSTANCE));
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    Mockito.when(configurationsClient.getRulesFromConfiguration(any(OkapiConnectionParams.class)))
      .thenReturn(Collections.emptyList());
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_record_with_only_holdings_and_items.json");
    String expectedMarcRecord = TestUtil.getMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  @Test
  void shouldMapInstanceHoldingsAndItem_to_marcRecord_whenMappingProfileTransformationsAreNotEmpty_AndSomeInstanceFieldProvided() throws FileNotFoundException {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createHoldingsAndItemSimpleFieldTransformations());
    mappingProfile.getTransformations().addAll(createInstanceFieldsTransformation());
    mappingProfile.setRecordTypes(Collections.singletonList(INSTANCE));
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    Mockito.when(configurationsClient.getRulesFromConfiguration(any(OkapiConnectionParams.class)))
      .thenReturn(Collections.emptyList());
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_record_with_only_holdings_and_items_and_instances.json");
    String expectedMarcRecord = TestUtil.getMarcFromJson(expectedJsonRecords);
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
    Assert.assertEquals(40, appendedMarcRecords.stream().map(vf -> vf.getTag()).collect(Collectors.toSet()).size());
    Assert.assertEquals(49, appendedMarcRecords.size());
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

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_record_with_only_holdings_and_items.json");
    String expectedMarcRecord = TestUtil.getMarcFromJson(expectedJsonRecords);
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
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);

    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_instance_transformationFields.json");
    String expectedMarcRecord = TestUtil.getMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  /**
   * This test makes sure if the path specified in transformation Fields, is correct and parsable,
   * by creating the mapping profile from the transformation fields for all Holdings records
   */
  @Test
  void shouldMapHoldings_to_marcRecord_withMappingProfileFromTransformationFields() throws FileNotFoundException {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_Holdings.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createHoldingsTransformationsFromTransformationFields());
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    Mockito.when(configurationsClient.getRulesFromConfiguration(any(OkapiConnectionParams.class)))
      .thenReturn(Collections.emptyList());

    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);

    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);

    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_holdings_transformationFields.json");
    String expectedMarcRecord = TestUtil.getMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  /**
   * This test makes sure if the path specified in transformation Fields, is correct and parsable,
   * by creating the mapping profile from the transformation fields for all Items records
   */
  @Test
  void shouldMapItems_to_marcRecord_withMappingProfileFromTransformationFields() throws FileNotFoundException {
    // given
    JsonObject instance = new JsonObject(readFileContentFromResources("mapping/given_inventory_instance.json"));
    List<JsonObject> instances = Collections.singletonList(instance);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setTransformations(createItemTransformationsFromTransformationFields());
    Mockito.when(referenceDataProvider.get(jobExecutionId, params))
      .thenReturn(referenceData);
    Mockito.when(configurationsClient.getRulesFromConfiguration(any(OkapiConnectionParams.class)))
      .thenReturn(Collections.emptyList());
    // when
    List<String> actualMarcRecords = mappingService.map(instances, mappingProfile, jobExecutionId, params);
    // then
    Assert.assertEquals(1, actualMarcRecords.size());
    String actualMarcRecord = actualMarcRecords.get(0);
    File expectedJsonRecords = getFileFromResources("mapping/expected_marc_item_transformationFields.json");
    String expectedMarcRecord = TestUtil.getMarcFromJson(expectedJsonRecords);
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  private List<Transformations> createHoldingsAndItemSimpleFieldTransformations() {
    List<Transformations> transformations = new ArrayList<>();
    transformations.add(createTransformations(CALLNUMBER_PREFIX_FIELD_ID, CALLNUMBER_PREFIX_FIELD_PATH, "900ff$b", HOLDINGS));
    transformations.add(createTransformations(CALLNUMBER_FIELD_ID, CALLNUMBER_FIELD_PATH, "900ff$a", HOLDINGS));
    transformations.add(createTransformations(CALLNUMBER_SUFFIX_FIELD_ID, CALLNUMBER_SUFFIX_FIELD_PATH, "901  $a", HOLDINGS));
    transformations.add(createTransformations(ELECTRONIC_ACCESS_LINKTEXT_FIELD_ID, HOLDINGS_ELECTRONIC_ACCESS_LINK_TEXT_PATH, "902  $a", HOLDINGS));
    transformations.add(createTransformations(ELECTRONIC_ACCESS_URI_FIELD_ID, HOLDINGS_ELECTRONIC_ACCESS_URI_PATH, "90312$a", HOLDINGS));
    transformations.add(createTransformations(EFFECTIVECALLNUMBER_CALL_NUMBER_FIELD_ID, ITEMS_EFFECTIVE_CALL_NUMBER_PATH, "904  $a", ITEM));
    transformations.add(createTransformations(ELECTRONIC_ACCESS_LINKTEXT_FIELD_ID, ITEMS_ELECTRONIC_ACCESS_LINK_TEXT_PATH, "905  $a", ITEM));
    transformations.add(createTransformations(ELECTRONIC_ACCESS_URI_FIELD_ID, ITEMS_ELECTRONIC_ACCESS_URI_PATH, "9061 $a", ITEM));
    transformations.add(createTransformations(MATERIALTYPE_FIELD_ID, MATERIAL_TYPE_ID_PATH, "907  $a", ITEM));
    transformations.add(createTransformations("holdings.permanentlocation.name", "$.holdings[*].permanentLocationId", "908  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.permanentlocation.code", "$.holdings[*].permanentLocationId", "909  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.permanentlocation.library.name", "$.holdings[*].permanentLocationId", "910  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.permanentlocation.library.code", "$.holdings[*].permanentLocationId", "911  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.permanentlocation.campus.name", "$.holdings[*].permanentLocationId", "912  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.permanentlocation.campus.code", "$.holdings[*].permanentLocationId", "913  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.permanentlocation.institution.name", "$.holdings[*].permanentLocationId", "914  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.permanentlocation.institution.code", "$.holdings[*].permanentLocationId", "915  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.temporarylocation.name", "$.holdings[*].temporaryLocationId", "916  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.temporarylocation.code", "$.holdings[*].temporaryLocationId", "917  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.temporarylocation.library.name", "$.holdings[*].temporaryLocationId", "918  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.temporarylocation.library.code", "$.holdings[*].temporaryLocationId", "919  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.temporarylocation.campus.name", "$.holdings[*].temporaryLocationId", "920  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.temporarylocation.campus.code", "$.holdings[*].temporaryLocationId", "921  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.temporarylocation.institution.name", "$.holdings[*].temporaryLocationId", "922  $a", HOLDINGS));
    transformations.add(createTransformations("holdings.temporarylocation.institution.code", "$.holdings[*].temporaryLocationId", "923  $a", HOLDINGS));
    transformations.add(createTransformations("item.permanentlocation.name", "$.holdings[*].items[*].permanentLocationId", "924  $a", ITEM));
    transformations.add(createTransformations("item.permanentlocation.code", "$.holdings[*].items[*].permanentLocationId", "925  $a", ITEM));
    transformations.add(createTransformations("item.permanentlocation.library.name", "$.holdings[*].items[*].permanentLocationId", "926  $a", ITEM));
    transformations.add(createTransformations("item.permanentlocation.library.code", "$.holdings[*].items[*].permanentLocationId", "927  $a", ITEM));
    transformations.add(createTransformations("item.permanentlocation.campus.name", "$.holdings[*].items[*].permanentLocationId", "928  $a", ITEM));
    transformations.add(createTransformations("item.permanentlocation.campus.code", "$.holdings[*].items[*].permanentLocationId", "929  $a", ITEM));
    transformations.add(createTransformations("item.permanentlocation.institution.name", "$.holdings[*].items[*].permanentLocationId", "930  $a", ITEM));
    transformations.add(createTransformations("item.permanentlocation.institution.code", "$.holdings[*].items[*].permanentLocationId", "931  $a", ITEM));
    transformations.add(createTransformations("item.effectivelocation.name", "$.holdings[*].items[*].effectiveLocationId", "932  $a", ITEM));
    transformations.add(createTransformations("item.effectivelocation.code", "$.holdings[*].items[*].effectiveLocationId", "933  $a", ITEM));
    transformations.add(createTransformations("item.effectivelocation.library.name", "$.holdings[*].items[*].effectiveLocationId", "934  $a", ITEM));
    transformations.add(createTransformations("item.effectivelocation.library.code", "$.holdings[*].items[*].effectiveLocationId", "935  $a", ITEM));
    transformations.add(createTransformations("item.effectivelocation.campus.name", "$.holdings[*].items[*].effectiveLocationId", "936  $a", ITEM));
    transformations.add(createTransformations("item.effectivelocation.campus.code", "$.holdings[*].items[*].effectiveLocationId", "937  $a", ITEM));
    transformations.add(createTransformations("item.effectivelocation.institution.name", "$.holdings[*].items[*].effectiveLocationId", "938  $a", ITEM));
    transformations.add(createTransformations("item.effectivelocation.institution.code", "$.holdings[*].items[*].effectiveLocationId", "939  $a", ITEM));
    return transformations;
  }

  private List<Transformations> createInstanceFieldsTransformation() {
    List<Transformations> transformations = new ArrayList<>();
    transformations.add(createTransformations(INSTANCE_HR_ID_FIELD_ID, INSTANCE_HR_ID_FIELD_PATH, "001", INSTANCE));
    transformations.add(createTransformations(INSTANCE_METADATA_UPDATED_DATE_FIELD_ID, INSTANCE_METADATA_UPDATED_DATE_FIELD_PATH, "005", INSTANCE));
    transformations.add(createTransformations(INSTANCE_METADATA_CREATED_DATE_FIELD_ID, INSTANCE_METADATA_CREATED_DATE_FIELD_PATH, "008", INSTANCE));
    transformations.add(createTransformations(INSTANCE_ELECTRONIC_ACCESS_URI_FIELD_ID, INSTANCE_ELECTRONIC_ACCESS_URI_FIELD_PATH, "8564 $u", INSTANCE));
    transformations.add(createTransformations(INSTANCE_ELECTRONIC_ACCESS_LINK_TEXT_FIELD_ID, INSTANCE_ELECTRONIC_ACCESS_LINK_TEXT_PATH, EMPTY, INSTANCE));
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

  /**
   * Construct the mapping profile Transformations from the fields from the Transformation Fields
   * (that are usually accessed on the UI via an API)
   */
  private List<Transformations> createHoldingsTransformationsFromTransformationFields() {
    List<Transformations> transformations = new ArrayList<>();
    AtomicInteger tag = new AtomicInteger(899);
    TransformationFieldCollection transformationFields = new JsonObject(
        readFileContentFromResources("mapping/expectedTransformationFields.json")).mapTo(TransformationFieldCollection.class);
    transformationFields.getTransformationFields().stream()
      .filter(tfn -> tfn.getRecordType().equals(TransformationField.RecordType.HOLDINGS))
      .forEach(tfn -> {
        String idx = tag.incrementAndGet() + "ff$a";
        transformations.add(createTransformations(tfn.getFieldId(), tfn.getPath(), idx, RecordType.fromValue(tfn.getRecordType()
          .toString())));
      });

    return transformations;
  }

  /**
   * Construct the mapping profile Transformations for Items from the fields from the Transformation Fields
   * (that are usually accessed on the UI via an API)
   */
  private List<Transformations> createItemTransformationsFromTransformationFields() {
    List<Transformations> transformations = new ArrayList<>();
    AtomicInteger tag = new AtomicInteger(924);
    TransformationFieldCollection transformationFields = new JsonObject(
      readFileContentFromResources("mapping/expectedTransformationFields.json")).mapTo(TransformationFieldCollection.class);
    transformationFields.getTransformationFields().stream()
      .filter(tfn -> tfn.getRecordType().equals(TransformationField.RecordType.ITEM))
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

