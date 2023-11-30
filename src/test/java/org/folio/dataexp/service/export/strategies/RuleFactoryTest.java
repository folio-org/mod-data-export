package org.folio.dataexp.service.export.strategies;

import com.google.common.collect.ImmutableList;
import org.assertj.core.util.Lists;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleFactoryTest extends BaseDataExportInitializer {
  private static final String DEFAULT_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";
  private static final String DEFAULT_RULE_FIELD_VALUE = "001";
  private static final String DEFAULT_HOLDING_RULE_FIELD_VALUE = "002";
  private static final String DEFAULT_RULE_DESCRIPTION = "defaultRuleDescription";
  private static final String DEFAULT_HOLDING_RULE_DESCRIPTION = "defaultHoldingRuleDescription";
  private static final String DEFAULT_RULE_FROM_VALUE = "defaultFromValue";
  private static final String DEFAULT_HOLDING_RULE_FROM_VALUE = "defaultHoldingFromValue";
  private static final String FIELD_ID_1 = "fieldId1";
  private static final String FIELD_ID_2 = "fieldId2";
  private static final String DEFAULT_RULE_ID = "defaultRuleId";
  private static final String DEFAULT_HOLDING_RULE_ID = "defaultHoldingRuleId";
  private static final String TRANSFORMATIONS_PATH_1 = "transformationsPath1";
  private static final String TRANSFORMATION_FIELD_VALUE_1 = "002";
  private static final String TRANSFORMATION_FIELD_VALUE_set_SUBFIELD = "002  $a";
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
  private static final Map<String, String> METADATA = Map.of(METADATA_CREATED_DATE, METADATA_CREATED_DATE_VALUE);

  @Autowired
  private RuleFactory ruleFactory;

  @Autowired
  @Qualifier("defaultRules")
  private List<Rule> defaultRulesFromConfigFile;

  @Autowired
  @Qualifier("holdingsDefaultRules")
  private List<Rule> defaultHoldingsRulesFromConfigFile;

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsNull() {
    // when
    List<Rule> rules = ruleFactory.create(null);

    // then
    assertEquals(defaultRulesFromConfigFile, rules);
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsDefault() {
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
  void shouldReturnDefaultRules_whenMappingProfileTransformationsIsEmpty() {
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
  void shouldReturnDefaultHoldingRules_whenMappingProfileTransformationsIsEmpty_HoldingRecordType() {
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
  void shouldReturnDefaultInstanceAndHoldingRules_whenMappingProfileTransformationsIsEmpty_HoldingAndInstanceRecordTypes() {
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
  void shouldReturnEmptyRules_whenMappingProfileTransformationsIsNotEnabled() {
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
  void shouldReturnEmptyRules_whenMappingProfileTransformationsPathIsEmpty() {
    // given
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setPath(EMPTY);
    transformations.setRecordType(RecordTypes.HOLDINGS);

    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(ImmutableList.of(transformations));
    mappingProfile.setRecordTypes(singletonList(RecordTypes.INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldReturnEmptyRules_whenMappingProfileTransformationsFieldIdIsEmpty() {
    // given
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setPath(TRANSFORMATIONS_PATH_1);
    transformations.setTransformation(EMPTY);
    transformations.setRecordType(RecordTypes.INSTANCE);
    transformations.setFieldId(EMPTY);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(ImmutableList.of(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldReturnDefaultRule_whenTransformationsValueIsEmpty_andTransformationIdEqualsDefaultRuleId() {
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
    mappingProfile.setTransformations(ImmutableList.of(transformations));
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
  void shouldReturnRulesWithOneTransformationRule_whenMappingProfileTransformationsContainsValueWithoutSubfield() {
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
  void shouldReturnRulesWithTwoTransformationRules_whenMappingProfileTransformationsContainsValueWithoutSubfield() {
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
  void shouldReturnRulesWithOneTransformationRule_whenTransformationsValueWithSubfieldAndIndicators() {
    // given
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setPath(TRANSFORMATIONS_PATH_1);
    transformations.setFieldId(FIELD_ID_1);
    transformations.setTransformation(TRANSFORMATION_FIELD_VALUE_set_SUBFIELD);
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
    assertEquals(SET_VALUE_FUNCTION, rules.get(0).getDataSources().get(1).getTranslation().getFunction());
    assertEquals(SPACE, rules.get(0).getDataSources().get(1).getTranslation().getParameter(VALUE_PARAMETER));
    assertEquals(SECOND_INDICATOR, rules.get(0).getDataSources().get(2).getIndicator());
    assertEquals(SET_VALUE_FUNCTION, rules.get(0).getDataSources().get(2).getTranslation().getFunction());
    assertEquals(SPACE, rules.get(0).getDataSources().get(2).getTranslation().getParameter(VALUE_PARAMETER));
  }

  @Test
  void shouldReturnTransformationRulesetPermanentLocationTranslation() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldNotReturnTransformationRulesetPermanentLocationTranslation_whenPermanentLocationEqualsTemporaryLocation() {
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
    mappingProfile.setTransformations(Lists.newArrayList(permanentLocationTransformations, temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TEMPORARY_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnTransformationRulesetTemporaryLocationTranslation() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForCodeField() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(CODE_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForLibraryName() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(NAME_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(LIBRARIES, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(LIBRARY_ID_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForLibraryCode() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(CODE_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(LIBRARIES, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(LIBRARY_ID_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }


  @Test
  void shouldReturnTransformationRulesetEffectiveLocationTranslation() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForCampusName() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(NAME_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(CAMPUSES, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(CAMPUS_ID_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForCampusCode() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(CODE_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(CAMPUSES, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(CAMPUS_ID_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForInstitutionName() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(NAME_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(INSTITUTIONS, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(INSTITUTION_ID_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnPermanentLocationRulesetTranslationForInstitutionCode() {
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
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertEquals(CODE_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(FIELD_KEY));
    assertEquals(INSTITUTIONS, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_KEY));
    assertEquals(INSTITUTION_ID_FIELD, rules.get(0).getDataSources().get(0).getTranslation().getParameter(REFERENCE_DATA_ID_FIELD_KEY));
  }

  @Test
  void shouldReturnRulesetDefaultTranslationWhenFieldIdContainsOneWord() {
    // given
    Transformations temporaryLocationTransformations = new Transformations();
    temporaryLocationTransformations.setEnabled(true);
    temporaryLocationTransformations.setFieldId(ONE_WORD_LOCATION_FIELD_ID);
    temporaryLocationTransformations.setPath(PERMANENT_LOCATION_PATH);
    temporaryLocationTransformations.setTransformation(TRANSFORMATION_FIELD_VALUE_1);
    temporaryLocationTransformations.setRecordType(RecordTypes.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile .setTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
    assertNull(rules.get(0).getDataSources().get(0).getTranslation().getParameters());
  }

  @Test
  void shouldReturnTransformationRulesetMaterialTypeTranslation() {
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
    assertEquals(SET_MATERIAL_TYPE_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnSingleRule_WhenTransformationHasMultipleSubFieldssetSameFieldId() {
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
    List<Transformations> transformations = Lists.newArrayList(transformation1, transformation2, transformation3);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(transformations);
    //when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    //then
    List<Rule> ruleList = rules.stream().filter(rule -> rule.getField().equals("900")).collect(Collectors.toList());
    assertEquals(1, ruleList.size());
    //3 data sources for subfields $a, $b, $c and 2 for indicators
    assertEquals(5, ruleList.get(0).getDataSources().size());
  }

  @Test
  void shouldReturnDifferentRules_WhenTransformationHasMultipleSubFieldssetSameFieldIdButDifferentIndicators() {
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
    List<Transformations> transformations = Lists.newArrayList(transformation1, transformation2, transformation3);
    MappingProfile mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setTransformations(transformations);
    //when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    //then
    List<Rule> ruleList = rules.stream().filter(rule -> rule.getField().equals("900")).collect(Collectors.toList());
    assertEquals(3, ruleList.size());
    //3 data sources for subfields $a, $b, $c and 2 for indicators
    assertEquals(3, ruleList.get(0).getDataSources().size());
    //the first field's indicators are used
    assertEquals(2, ruleList.get(0).getDataSources().stream().
      filter(ds -> ds.getIndicator() != null && ds.getTranslation().getParameter("value").equals("f")).count());
  }

  @Test
  void shouldReturnCombinedRule_whenTransformationIsEmpty_andDefaultRuleHasMultipleSubfields() {
    // given
    List<Rule> defaultRules = setUpElectorincAccesDefaultRuleWithIdicators();
    //ToDO
    //doReturn(defaultRules).when(ruleFactory).getDefaultRulesFromFile();
    Transformations transformation = new Transformations();
    transformation.setEnabled(true);
    transformation.setFieldId("instance.electronic.access.linktext.related.resource");
    transformation.setPath("$.instance.electronicAccess[?(@.relationshipId=='5bfe1b7b-f151-4501-8cfa-23b321d5cd1e')].linkText");
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
    assertEquals("$.instance.electronicAccess[?(@.relationshipId=='5bfe1b7b-f151-4501-8cfa-23b321d5cd1e')].linkText", rules.get(0).getDataSources().get(0).getFrom());
    assertEquals("y", rules.get(0).getDataSources().get(0).getSubfield());
    assertEquals("1", rules.get(0).getDataSources().get(1).getIndicator());
    assertEquals("2", rules.get(0).getDataSources().get(2).getIndicator());
  }

  @Test
  void shouldNotReturnCombinedRule_whenDefaultRulesDontContainTransformationField() {
    // given
    Transformations transformation = new Transformations();
    transformation.setEnabled(true);
    transformation.setFieldId("instance.electronic.access.linktext.related.resource");
    transformation.setPath("$.instance.electronicAccess[?(@.relationshipId=='5bfe1b7b-f151-4501-8cfa-23b321d5cd1e')].linkText");
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
  void shouldNotReturnCombinedRule_whenEmptyTransformationFieldDoesntMatchDefaultSubfieldRuleId() {
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
  void shouldReturnDefaultRuleWithHoldingsAndItemRules_whenMappingProfileIsDefault_andContainsHoldingsAndItemTransformations() {
    int transformationRulesAmount = 2;
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
    mappingProfile.setTransformations(ImmutableList.of(holdingsTransformations, itemTransformations));
    mappingProfile.setRecordTypes(ImmutableList.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    var defaultRulesAmount = defaultRulesFromConfigFile.size() + defaultHoldingsRulesFromConfigFile.size();
    assertEquals(defaultRulesAmount + transformationRulesAmount, rules.size());
  }

  @Test
  void shouldReturnDefaultRuleWithItemRulesWithMetadata_whenMappingProfileIsDefault_andContainsItemTransformationsWithMetadata() {
    int transformationRulesAmount = 1;
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
    mappingProfile.setTransformations(ImmutableList.of(itemTransformation));
    mappingProfile.setRecordTypes(ImmutableList.of(RecordTypes.INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);
    // then
    assertEquals(transformationRulesAmount + defaultRulesFromConfigFile.size(), rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TRANSFORMATIONS_PATH_1, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(METADATA_CREATED_DATE_VALUE, rules.get(0).getMetadata().getData().get(METADATA_CREATED_DATE).getFrom());

    assertTrue(rules.containsAll(defaultRulesFromConfigFile));
  }

  private List<Rule> setUpElectorincAccesDefaultRuleWithIdicators() {
    DataSource linkTextDataSource = new DataSource();
    linkTextDataSource.setFrom("$.instance.electronicAccess[*].linkText");
    linkTextDataSource.setSubfield("y");
    DataSource uriDataSource = new DataSource();
    uriDataSource.setFrom("$.instance.electronicAccess[*].uri");
    uriDataSource.setSubfield("u");
    DataSource indicator1DataSource = new DataSource();
    indicator1DataSource.setIndicator("1");
    DataSource indicator2DataSource = new DataSource();
    indicator2DataSource.setIndicator("2");
    Rule defaultRule = new Rule();
    defaultRule.setId("instance.electronic.access");
    defaultRule.setField("856");
    defaultRule.setDescription("Electronic access");
    defaultRule.setDataSources(Lists.newArrayList(linkTextDataSource, indicator1DataSource, indicator2DataSource));
    return Lists.newArrayList(defaultRule);
  }
}
