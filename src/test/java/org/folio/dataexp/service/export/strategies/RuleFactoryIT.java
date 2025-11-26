package org.folio.dataexp.service.export.strategies;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.CALLNUMBER_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.CALLNUMBER_FIELD_PATH;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.CALLNUMBER_PREFIX_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.CALLNUMBER_PREFIX_FIELD_PATH;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.CALLNUMBER_SUFFIX_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.CALLNUMBER_SUFFIX_FIELD_PATH;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.EFFECTIVE_LOCATION_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.EFFECTIVE_LOCATION_PATH;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.MATERIAL_TYPE_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.MATERIAL_TYPE_PATH;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.ONE_WORD_LOCATION_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_CAMPUS_CODE_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_CAMPUS_NAME_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_CODE_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_INSTITUTION_CODE_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_INSTITUTION_NAME_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_LIBRARY_CODE_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_LIBRARY_NAME_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.PERMANENT_LOCATION_PATH;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.SET_LOCATION_FUNCTION;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.SET_MATERIAL_TYPE_FUNCTION;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.TEMPORARY_LOCATION_FIELD_ID;
import static org.folio.dataexp.service.export.strategies.TransformationConstants.TEMPORARY_LOCATION_PATH;
import static org.folio.dataexp.service.export.strategies.translation.builder.LocationTranslationBuilder.CAMPUSES;
import static org.folio.dataexp.service.export.strategies.translation.builder.LocationTranslationBuilder.INSTITUTIONS;
import static org.folio.dataexp.service.export.strategies.translation.builder.LocationTranslationBuilder.LIBRARIES;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class RuleFactoryIT extends BaseDataExportInitializerIT {
  private static final String DEFAULT_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";
  private static final String FIELD_ID_1 = "fieldId1";
  private static final String FIELD_ID_2 = "fieldId2";
  private static final String TRANSFORMATIONS_PATH_1 = "transformationsPath1";
  private static final String TRANSFORMATION_FIELD_VALUE_1 = "002";
  private static final String TRANSFORMATION_FIELD_VALUE_SET_SUBFIELD = "002  $a";
  private static final String TRANSFORMATIONS_PATH_2 = "transformationsPath2";
  private static final String TRANSFORMATION_FIELD_VALUE_2 = "003";
  private static final String SUBFIELD_A = "a";
  private static final String FIRST_INDICATOR = "1";
  private static final String SET_VALUE_FUNCTION = "set_value";
  private static final String VALUE_PARAMETER = "value";
  private static final String SECOND_INDICATOR = "2";
  private static final String NAME_FIELD = "name";
  private static final String CAMPUS_ID_FIELD = "campusId";
  private static final String FIELD_KEY = "field";
  private static final String REFERENCE_DATA_KEY = "referenceData";
  private static final String REFERENCE_DATA_ID_FIELD_KEY = "referenceDataIdField";
  private static final String CODE_FIELD = "code";
  private static final String INSTITUTION_ID_FIELD = "institutionId";
  private static final String LIBRARY_ID_FIELD = "libraryId";
  private static final String METADATA_CREATED_DATE = "created date";
  private static final String METADATA_CREATED_DATE_VALUE = "2021-07-15T11:07:49.212+00:00";
  private static final Map<String, String> METADATA =
      Map.of(METADATA_CREATED_DATE, METADATA_CREATED_DATE_VALUE);

  @Autowired private RuleFactory ruleFactory;

  @Autowired private List<Rule> defaultRulesFromConfigFile;

  @Autowired private List<Rule> defaultHoldingsRulesFromConfigFile;

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsNull() throws TransformationRuleException {
    // when
    List<Rule> rules = ruleFactory.create(null);

    // then
    assertEquals(defaultRulesFromConfigFile, rules);
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsDefault() throws TransformationRuleException {
    // given
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString(DEFAULT_MAPPING_PROFILE_ID));
    mappingProfile.setRecordTypes(singletonList(RecordTypes.INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(defaultRulesFromConfigFile, rules);
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileTransformationsIsEmpty()
      throws TransformationRuleException {
    // given
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setRecordTypes(singletonList(RecordTypes.INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(defaultRulesFromConfigFile, rules);
  }

  @Test
  void shouldReturnDefaultHoldingRulesWhenMappingProfileTransformationsIsEmptyHoldingRecordType()
      throws TransformationRuleException {
    // given
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setRecordTypes(singletonList(RecordTypes.HOLDINGS));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(defaultHoldingsRulesFromConfigFile, rules);
  }

  @Test
  void
      shouldReturnDefaultInstanceAndHoldingRulesWhenMappingProfileTransformationsIsEmptyHoldingAndInstanceRecordTypes()
          throws TransformationRuleException {
    // given
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS, RecordTypes.INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    var expected = new ArrayList<>();
    expected.addAll(defaultRulesFromConfigFile);
    expected.addAll(defaultHoldingsRulesFromConfigFile);

    assertEquals(expected, rules);
  }

  @Test
  void shouldReturnEmptyRules_whenMappingProfileTransformationsIsNotEnabled()
      throws TransformationRuleException {
    // given
    Transformations transformations = new Transformations();
    transformations.setEnabled(false);
    transformations.setRecordType(RecordTypes.INSTANCE);

    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE));
    mappingProfile.setTransformations(singletonList(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldReturnEmptyRules_whenMappingProfileTransformationsPathIsEmpty()
      throws TransformationRuleException {
    // given
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setPath(EMPTY);
    transformations.setRecordType(RecordTypes.HOLDINGS);

    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(List.of(transformations));
    mappingProfile.setRecordTypes(singletonList(RecordTypes.INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldReturnEmptyRules_whenMappingProfileTransformationsFieldIdIsEmpty()
      throws TransformationRuleException {
    // given
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setPath(TRANSFORMATIONS_PATH_1);
    transformations.setTransformation(EMPTY);
    transformations.setRecordType(RecordTypes.INSTANCE);
    transformations.setFieldId(EMPTY);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(List.of(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void
      shouldReturnDefaultRule_whenTransformationsValueIsEmpty_andTransformationIdEqualsDefaultRuleId()
          throws TransformationRuleException {
    // given
    var existDefaultRuleId = "instance.metadata.updateddate";
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setPath(TRANSFORMATIONS_PATH_1);
    transformations.setFieldId(existDefaultRuleId);
    transformations.setTransformation(EMPTY);
    transformations.setRecordType(RecordTypes.INSTANCE);

    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(List.of(transformations));
    mappingProfile.setRecordTypes(singletonList(RecordTypes.INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(existDefaultRuleId, rules.get(0).getId());
    assertEquals("005", rules.get(0).getField());
    assertEquals("Date and Time of Latest Transaction", rules.get(0).getDescription());
    assertEquals("$.instance.metadata.updatedDate", rules.get(0).getDataSources().get(0).getFrom());
  }

  @Test
  void
      shouldReturnRulesWithOneTransformationRule_whenMappingProfileTransformationsContainsValueWithoutSubfield()
          throws TransformationRuleException {
    // given
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setPath(TRANSFORMATIONS_PATH_1);
    transformations.setFieldId(FIELD_ID_1);
    transformations.setRecordType(RecordTypes.HOLDINGS);
    transformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TRANSFORMATIONS_PATH_1, rules.get(0).getDataSources().get(0).getFrom());
  }

  @Test
  void
      shouldReturnRulesWithTwoTransformationRules_whenMappingProfileTransformationsContainsValueWithoutSubfield()
          throws TransformationRuleException {
    // given
    Transformations transformations1 = new Transformations();
    transformations1.setEnabled(true);
    transformations1.setPath(TRANSFORMATIONS_PATH_1);
    transformations1.setFieldId(FIELD_ID_1);
    transformations1.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    transformations1.setRecordType(RecordTypes.ITEM);
    Transformations transformations2 = new Transformations();
    transformations2.setEnabled(true);
    transformations2.setPath(TRANSFORMATIONS_PATH_2);
    transformations2.setFieldId(FIELD_ID_2);
    transformations2.setTransformation(TRANSFORMATION_FIELD_VALUE_2);
    transformations2.setRecordType(RecordTypes.ITEM);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(transformations1, transformations2));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(2, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TRANSFORMATIONS_PATH_1, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(TRANSFORMATION_FIELD_VALUE_2, rules.get(1).getField());
    assertEquals(TRANSFORMATIONS_PATH_2, rules.get(1).getDataSources().get(0).getFrom());
  }

  @Test
  void
      shouldReturnRulesWithOneTransformationRule_whenTransformationsValueWithSubfieldAndIndicators()
          throws TransformationRuleException {
    // given
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setPath(TRANSFORMATIONS_PATH_1);
    transformations.setFieldId(FIELD_ID_1);
    transformations.setTransformation(TRANSFORMATION_FIELD_VALUE_SET_SUBFIELD);
    transformations.setRecordType(RecordTypes.ITEM);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TRANSFORMATIONS_PATH_1, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(SUBFIELD_A, rules.get(0).getDataSources().get(0).getSubfield());
    assertEquals(FIRST_INDICATOR, rules.get(0).getDataSources().get(1).getIndicator());
    assertEquals(
        SET_VALUE_FUNCTION, rules.get(0).getDataSources().get(1).getTranslation().getFunction());
    assertEquals(
        SPACE, rules.get(0).getDataSources().get(1).getTranslation().getParameter(VALUE_PARAMETER));
    assertEquals(SECOND_INDICATOR, rules.get(0).getDataSources().get(2).getIndicator());
    assertEquals(
        SET_VALUE_FUNCTION, rules.get(0).getDataSources().get(2).getTranslation().getFunction());
    assertEquals(
        SPACE, rules.get(0).getDataSources().get(2).getTranslation().getParameter(VALUE_PARAMETER));
  }

  @Test
  void shouldReturnTransformationRulesetPermanentLocationTranslation()
      throws TransformationRuleException {
    // given
    Transformations permanentLocationTransformations = new Transformations();
    permanentLocationTransformations.setEnabled(true);
    permanentLocationTransformations.setFieldId(PERMANENT_LOCATION_FIELD_ID);
    permanentLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    permanentLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    permanentLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(permanentLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void
      shouldNotReturnTransformationRulesetPermanentLocationTranslation_whenPermanentLocationEqualsTemporaryLocation()
          throws TransformationRuleException {
    // given
    Transformations permanentLocationTransformations = new Transformations();
    permanentLocationTransformations.setEnabled(true);
    permanentLocationTransformations.setFieldId(PERMANENT_LOCATION_FIELD_ID);
    permanentLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    permanentLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    permanentLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(TEMPORARY_LOCATION_FIELD_ID);
    temporaryLocationTransformations.setPath(TEMPORARY_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(
        Lists.newArrayList(permanentLocationTransformations, temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TEMPORARY_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnTransformationRulesetTemporaryLocationTranslation()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(TEMPORARY_LOCATION_FIELD_ID);
    temporaryLocationTransformations.setPath(TEMPORARY_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TEMPORARY_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForCodeField()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(PERMANENT_LOCATION_CODE_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(
        CODE_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForLibraryName()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(PERMANENT_LOCATION_LIBRARY_NAME_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(
        NAME_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(
        LIBRARIES,
        rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(
        LIBRARY_ID_FIELD,
        rules
            .get(0)
            .getDataSources()
            .get(0)
            .getTranslation()
            .getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForLibraryCode()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(PERMANENT_LOCATION_LIBRARY_CODE_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(
        CODE_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(
        LIBRARIES,
        rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(
        LIBRARY_ID_FIELD,
        rules
            .get(0)
            .getDataSources()
            .get(0)
            .getTranslation()
            .getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnTransformationRulesetEffectiveLocationTranslation()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(EFFECTIVE_LOCATION_FIELD_ID);
    temporaryLocationTransformations.setPath(EFFECTIVE_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.ITEM);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(EFFECTIVE_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForCampusName()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(PERMANENT_LOCATION_CAMPUS_NAME_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(
        NAME_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(
        CAMPUSES,
        rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(
        CAMPUS_ID_FIELD,
        rules
            .get(0)
            .getDataSources()
            .get(0)
            .getTranslation()
            .getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForCampusCode()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(PERMANENT_LOCATION_CAMPUS_CODE_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(
        CODE_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(
        CAMPUSES,
        rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(
        CAMPUS_ID_FIELD,
        rules
            .get(0)
            .getDataSources()
            .get(0)
            .getTranslation()
            .getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForInstitutionName()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(PERMANENT_LOCATION_INSTITUTION_NAME_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(
        NAME_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(
        INSTITUTIONS,
        rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(
        INSTITUTION_ID_FIELD,
        rules
            .get(0)
            .getDataSources()
            .get(0)
            .getTranslation()
            .getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForInstitutionCode()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(PERMANENT_LOCATION_INSTITUTION_CODE_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(
        CODE_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(
        INSTITUTIONS,
        rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(
        INSTITUTION_ID_FIELD,
        rules
            .get(0)
            .getDataSources()
            .get(0)
            .getTranslation()
            .getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnRulesetDefaultTranslationWhenFieldIdContainsOneWord()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(ONE_WORD_LOCATION_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertNull(rules.get(0).getDataSources().get(0).getTranslation().getParameters());
  }

  @Test
  void shouldReturnTransformationRulesetMaterialTypeTranslation()
      throws TransformationRuleException {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(MATERIAL_TYPE_FIELD_ID);
    temporaryLocationTransformations.setPath(MATERIAL_TYPE_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.ITEM);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(MATERIAL_TYPE_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        SET_MATERIAL_TYPE_FUNCTION,
        rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnSingleRuleWhenTransformationHasMultipleSubFieldssetSameFieldId()
      throws TransformationRuleException {
    // given
    Transformations transformation1 = new Transformations();
    transformation1.setEnabled(true);
    transformation1.setFieldId(CALLNUMBER_FIELD_ID);
    transformation1.setPath(CALLNUMBER_FIELD_PATH);
    transformation1.setTransformation("900ff$a");
    transformation1.setRecordType(RecordTypes.ITEM);
    Transformations transformation2 = new Transformations();
    transformation2.setEnabled(true);
    transformation2.setFieldId(CALLNUMBER_PREFIX_FIELD_ID);
    transformation2.setPath(CALLNUMBER_PREFIX_FIELD_PATH);
    transformation2.setTransformation("900ff$b");
    transformation2.setRecordType(RecordTypes.ITEM);
    Transformations transformation3 = new Transformations();
    transformation3.setEnabled(true);
    transformation3.setFieldId(CALLNUMBER_SUFFIX_FIELD_ID);
    transformation3.setPath(CALLNUMBER_SUFFIX_FIELD_PATH);
    transformation3.setTransformation("900ff$c");
    transformation3.setRecordType(RecordTypes.ITEM);
    List<Transformations> transformations =
        Lists.newArrayList(transformation1, transformation2, transformation3);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(transformations);
    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    List<Rule> ruleList = rules.stream().filter(rule -> rule.getField().equals("900")).toList();
    assertEquals(1, ruleList.size());
    // 3 data sources for subfields $a, $b, $c and 2 for indicators
    assertEquals(5, ruleList.get(0).getDataSources().size());
  }

  @Test
  void
      shouldReturnDifferentRulesWhenTransformationHasMultipleSubFieldssetSameFieldIdButDifferentIndicators()
          throws TransformationRuleException {
    // given
    Transformations transformation1 = new Transformations();
    transformation1.setEnabled(true);
    transformation1.setFieldId(CALLNUMBER_FIELD_ID);
    transformation1.setPath(CALLNUMBER_FIELD_PATH);
    transformation1.setTransformation("900ff$a");
    transformation1.setRecordType(RecordTypes.ITEM);
    Transformations transformation2 = new Transformations();
    transformation2.setEnabled(true);
    transformation2.setFieldId(CALLNUMBER_PREFIX_FIELD_ID);
    transformation2.setPath(CALLNUMBER_PREFIX_FIELD_PATH);
    transformation2.setTransformation("900  $b");
    transformation2.setRecordType(RecordTypes.ITEM);
    Transformations transformation3 = new Transformations();
    transformation3.setEnabled(true);
    transformation3.setFieldId(CALLNUMBER_SUFFIX_FIELD_ID);
    transformation3.setPath(CALLNUMBER_SUFFIX_FIELD_PATH);
    transformation3.setTransformation("90011$c");
    transformation3.setRecordType(RecordTypes.ITEM);
    List<Transformations> transformations =
        Lists.newArrayList(transformation1, transformation2, transformation3);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(transformations);
    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    List<Rule> ruleList = rules.stream().filter(rule -> rule.getField().equals("900")).toList();
    assertEquals(3, ruleList.size());
    // 3 data sources for subfields $a, $b, $c and 2 for indicators
    assertEquals(3, ruleList.get(0).getDataSources().size());
    // the first field's indicators are used
    assertEquals(
        2,
        ruleList.get(0).getDataSources().stream()
            .filter(
                ds ->
                    ds.getIndicator() != null
                        && ds.getTranslation().getParameter("value").equals("f"))
            .count());
  }

  @Test
  void shouldReturnCombinedRule_whenTransformationIsEmpty_andDefaultRuleHasMultipleSubfields()
      throws TransformationRuleException {
    // given
    Transformations transformation = new Transformations();
    transformation.setEnabled(true);
    transformation.setFieldId("instance.electronic.access.linktext.related.resource");
    transformation.setPath(
        "$.instance.electronicAccess[?(@.relationshipId=='"
            + "5bfe1b7b-f151-4501-8cfa-23b321d5cd1e')].linkText");
    transformation.setTransformation(EMPTY);
    transformation.setRecordType(RecordTypes.INSTANCE);
    List<Transformations> transformations = Lists.newArrayList(transformation);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(transformations);
    mappingProfile.setRecordTypes(singletonList(RecordTypes.INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals("856", rules.get(0).getField());
    assertEquals(
        "$.instance.electronicAccess[?(@.relationshipId=='5bfe1"
            + "b7b-f151-4501-8cfa-23b321d5cd1e')].linkText",
        rules.get(0).getDataSources().get(0).getFrom());
    assertEquals("y", rules.get(0).getDataSources().get(0).getSubfield());
    assertEquals("1", rules.get(0).getDataSources().get(1).getIndicator());
    assertEquals("2", rules.get(0).getDataSources().get(2).getIndicator());
  }

  @Test
  void shouldNotReturnCombinedRule_whenDefaultRulesDontContainTransformationField()
      throws TransformationRuleException {
    // given
    Transformations transformation = new Transformations();
    transformation.setEnabled(true);
    transformation.setFieldId("instance.electronic.access.linktext.related.resource");
    transformation.setPath(
        "$.instance.electronicAccess[?(@.relationshipId=='5bfe1"
            + "b7b-f151-4501-8cfa-23b321d5cd1e')].linkText");
    transformation.setTransformation(EMPTY);
    transformation.setRecordType(RecordTypes.INSTANCE);
    List<Transformations> transformations = Lists.newArrayList(transformation);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(transformations);

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldNotReturnCombinedRule_whenEmptyTransformationFieldDoesntMatchDefaultSubfieldRuleId()
      throws TransformationRuleException {
    // given
    Transformations transformation = new Transformations();
    transformation.setEnabled(true);
    transformation.setFieldId("instance.electronic.access.nonexistingsubfield");
    transformation.setPath("$.instance.electronicAccess.nonexistingsubfield");
    transformation.setTransformation(EMPTY);
    transformation.setRecordType(RecordTypes.INSTANCE);
    List<Transformations> transformations = Lists.newArrayList(transformation);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(transformations);

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void
      shouldReturnDefaultRuleWithHoldingsAndItemRules_whenMappingProfileIsDefault_andContainsHoldingsAndItemTransformations()
          throws TransformationRuleException {
    // given
    Transformations holdingsTransformations = new Transformations();
    holdingsTransformations.setEnabled(true);
    holdingsTransformations.setPath(TRANSFORMATIONS_PATH_1);
    holdingsTransformations.setFieldId(FIELD_ID_1);
    holdingsTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    holdingsTransformations.setRecordType(RecordTypes.HOLDINGS);
    Transformations itemTransformations = new Transformations();
    itemTransformations.setEnabled(true);
    itemTransformations.setPath(TRANSFORMATIONS_PATH_2);
    itemTransformations.setFieldId(FIELD_ID_2);
    itemTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_2);
    itemTransformations.setRecordType(RecordTypes.ITEM);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString(DEFAULT_MAPPING_PROFILE_ID));
    mappingProfile.setTransformations(List.of(holdingsTransformations, itemTransformations));
    mappingProfile.setRecordTypes(
        List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    int transformationRulesAmount = 2;
    assertEquals(defaultRulesFromConfigFile.size() + transformationRulesAmount, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TRANSFORMATIONS_PATH_1, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(TRANSFORMATION_FIELD_VALUE_2, rules.get(1).getField());
    assertEquals(TRANSFORMATIONS_PATH_2, rules.get(1).getDataSources().get(0).getFrom());
    assertTrue(rules.containsAll(defaultRulesFromConfigFile));
  }

  @Test
  void
      shouldReturnDefaultRuleWithItemRulesWithMetadata_whenMappingProfileIsDefault_andContainsItemTransformationsWithMetadata()
          throws TransformationRuleException {
    // given
    Transformations itemTransformation = new Transformations();
    itemTransformation.setEnabled(true);
    itemTransformation.setPath(TRANSFORMATIONS_PATH_1);
    itemTransformation.setFieldId(FIELD_ID_1);
    itemTransformation.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    itemTransformation.setRecordType(RecordTypes.ITEM);
    itemTransformation.setMetadataParameters(METADATA);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString(DEFAULT_MAPPING_PROFILE_ID));
    mappingProfile.setTransformations(List.of(itemTransformation));
    mappingProfile.setRecordTypes(List.of(RecordTypes.ITEM));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);
    // then
    int transformationRulesAmount = 1;
    assertEquals(transformationRulesAmount + defaultRulesFromConfigFile.size(), rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TRANSFORMATIONS_PATH_1, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(
        METADATA_CREATED_DATE_VALUE,
        rules.get(0).getMetadata().getData().get(METADATA_CREATED_DATE).getFrom());

    assertTrue(rules.containsAll(defaultRulesFromConfigFile));
  }

  @Test
  @SneakyThrows
  void shouldSuppressListedFields() {
    assertTrue(defaultRulesFromConfigFile.stream().anyMatch(rule -> "008".equals(rule.getField())));
    assertTrue(defaultRulesFromConfigFile.stream().anyMatch(rule -> "020".equals(rule.getField())));
    assertTrue(defaultRulesFromConfigFile.stream().anyMatch(rule -> "856".equals(rule.getField())));

    var mappingProfile =
        MappingProfile.builder()
            .recordTypes(Collections.singletonList(RecordTypes.INSTANCE))
            .fieldsSuppression("008, 020 , 856")
            .build();
    var rules = ruleFactory.getRules(mappingProfile);

    assertTrue(rules.stream().noneMatch(rule -> "008".equals(rule.getField())));
    assertTrue(rules.stream().noneMatch(rule -> "020".equals(rule.getField())));
    assertTrue(rules.stream().noneMatch(rule -> "856".equals(rule.getField())));
  }

  @Test
  @SneakyThrows
  void shouldSuppress999ff() {
    var mappingProfile =
        MappingProfile.builder()
            .recordTypes(Collections.singletonList(RecordTypes.INSTANCE))
            .suppress999ff(true)
            .build();

    assertTrue(defaultRulesFromConfigFile.stream().anyMatch(rule -> "999".equals(rule.getField())));

    var rules = ruleFactory.getRules(mappingProfile);

    assertTrue(rules.stream().noneMatch(rule -> "999".equals(rule.getField())));
  }

  @Test
  void shouldReturnEmptyWhenTransformationDisabled() throws TransformationRuleException {
    // TestMate-1dfb07e8b9f2a2c06dd19cb997c33f7f
    // given
    var transformation = new Transformations();
    transformation.setEnabled(false);
    transformation.setFieldId("instance.metadata.updateddate");
    transformation.setRecordType(RecordTypes.INSTANCE);
    // when
    Optional<Rule> resultRule =
        ruleFactory.createDefaultByTransformations(transformation, defaultRulesFromConfigFile);
    // then
    assertTrue(resultRule.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(value = RecordTypes.class, names = "INSTANCE", mode = EnumSource.Mode.EXCLUDE)
  void shouldReturnEmptyWhenRecordTypeIsNotInstance(RecordTypes recordType)
      throws TransformationRuleException {
    // TestMate-24966dcec68c6c828e71838ac1cc4b97
    // given
    var transformation = new Transformations();
    transformation.setEnabled(true);
    transformation.setFieldId("some.field.id");
    transformation.setRecordType(recordType);
    // when
    Optional<Rule> resultRule =
        ruleFactory.createDefaultByTransformations(transformation, defaultRulesFromConfigFile);
    // then
    assertTrue(resultRule.isEmpty());
  }

  @Test
  void shouldReturnCombinedRuleWhenFieldIdMatchesCombinedBuilderKey()
      throws TransformationRuleException {
    // TestMate-da0ddb1f2f4195b6e61b248b41998f5f
    // given
    var transformation = new Transformations();
    transformation.setEnabled(true);
    transformation.setFieldId("instance.electronic.access.uri");
    transformation.setRecordType(RecordTypes.INSTANCE);
    // when
    Optional<Rule> resultRule =
        ruleFactory.createDefaultByTransformations(transformation, defaultRulesFromConfigFile);
    // then
    assertTrue(resultRule.isPresent());
    var rule = resultRule.get();
    assertEquals("856", rule.getField());
    assertTrue(rule.getDataSources().stream().anyMatch(ds -> "u".equals(ds.getSubfield())));
  }

  @Test
  void shouldReturnDefaultRuleWhenNoSpecialBuilderMatches() throws TransformationRuleException {
    // TestMate-d7627bed2afb20d231aa29f5fe6f25f1
    // given
    var existDefaultRuleId = "instance.metadata.updateddate";
    var transformation = new Transformations();
    transformation.setEnabled(true);
    transformation.setFieldId(existDefaultRuleId);
    transformation.setRecordType(RecordTypes.INSTANCE);
    transformation.setPath("$.instance.metadata.updatedDate");
    transformation.setTransformation(EMPTY);
    // when
    Optional<Rule> resultRule =
        ruleFactory.createDefaultByTransformations(transformation, defaultRulesFromConfigFile);
    // then
    assertTrue(resultRule.isPresent());
    var rule = resultRule.get();
    assertEquals(existDefaultRuleId, rule.getId());
    assertEquals("005", rule.getField());
    assertEquals("$.instance.metadata.updatedDate", rule.getDataSources().get(0).getFrom());
  }
}
