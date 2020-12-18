package org.folio.service.mapping;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.util.Lists;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.TestUtil.CALLNUMBER_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_FIELD_PATH;
import static org.folio.TestUtil.CALLNUMBER_PREFIX_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_PREFIX_FIELD_PATH;
import static org.folio.TestUtil.CALLNUMBER_SUFFIX_FIELD_ID;
import static org.folio.TestUtil.CALLNUMBER_SUFFIX_FIELD_PATH;
import static org.folio.TestUtil.EFFECTIVE_LOCATION_FIELD_ID;
import static org.folio.TestUtil.EFFECTIVE_LOCATION_PATH;
import static org.folio.TestUtil.MATERIAL_TYPE_FIELD_ID;
import static org.folio.TestUtil.MATERIAL_TYPE_PATH;
import static org.folio.TestUtil.ONE_WORD_LOCATION_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_CAMPUS_CODE_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_CAMPUS_NAME_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_CODE_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_INSTITUTION_CODE_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_INSTITUTION_NAME_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_LIBRARY_CODE_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_LIBRARY_NAME_FIELD_ID;
import static org.folio.TestUtil.PERMANENT_LOCATION_PATH;
import static org.folio.TestUtil.SET_LOCATION_FUNCTION;
import static org.folio.TestUtil.SET_MATERIAL_TYPE_FUNCTION;
import static org.folio.TestUtil.TEMPORARY_LOCATION_FIELD_ID;
import static org.folio.TestUtil.TEMPORARY_LOCATION_PATH;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.RecordType.ITEM;
import static org.folio.util.ExternalPathResolver.CAMPUSES;
import static org.folio.util.ExternalPathResolver.INSTITUTIONS;
import static org.folio.util.ExternalPathResolver.LIBRARIES;
import static org.mockito.Mockito.doReturn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.Silent.class)
@ExtendWith(MockitoExtension.class)
class RuleFactoryUnitTest {
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
  private static final String TRANSFORMATION_FIELD_VALUE_WITH_SUBFIELD = "002  $a";
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

  @Spy
  private static RuleFactory ruleFactory = new RuleFactory();

  private static List<Rule> defaultInstanceRules;
  private static List<Rule> defaultHoldingsRules;

  @BeforeEach
  public void setUp() {
    setUpDefaultRules();
    Mockito.lenient().doReturn(defaultInstanceRules).when(ruleFactory).getDefaultRulesFromFile();
    Mockito.lenient().doReturn(defaultHoldingsRules).when(ruleFactory).getDefaultHoldingsRulesFromFile();
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsNull() {
    // when
    List<Rule> rules = ruleFactory.create(null);

    // then
    assertEquals(1, rules.size());
    assertEquals(DEFAULT_RULE_FIELD_VALUE, rules.get(0).getField());
    assertEquals(DEFAULT_RULE_DESCRIPTION, rules.get(0).getDescription());
    assertEquals(DEFAULT_RULE_FROM_VALUE, rules.get(0).getDataSources().get(0).getFrom());
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsDefault() {
    // given
    MappingProfile mappingProfile = new MappingProfile()
      .withId(DEFAULT_MAPPING_PROFILE_ID)
      .withRecordTypes(singletonList(INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(DEFAULT_RULE_FIELD_VALUE, rules.get(0).getField());
    assertEquals(DEFAULT_RULE_DESCRIPTION, rules.get(0).getDescription());
    assertEquals(DEFAULT_RULE_FROM_VALUE, rules.get(0).getDataSources().get(0).getFrom());
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileTransformationsIsEmpty() {
    // given
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withRecordTypes(singletonList(INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(DEFAULT_RULE_FIELD_VALUE, rules.get(0).getField());
    assertEquals(DEFAULT_RULE_DESCRIPTION, rules.get(0).getDescription());
    assertEquals(DEFAULT_RULE_FROM_VALUE, rules.get(0).getDataSources().get(0).getFrom());
  }

  @Test
  void shouldReturnDefaultHoldingRules_whenMappingProfileTransformationsIsEmpty_HoldingRecordType() {
    // given
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withRecordTypes(singletonList(HOLDINGS));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(DEFAULT_HOLDING_RULE_FIELD_VALUE, rules.get(0).getField());
    assertEquals(DEFAULT_HOLDING_RULE_DESCRIPTION, rules.get(0).getDescription());
    assertEquals(DEFAULT_HOLDING_RULE_FROM_VALUE, rules.get(0).getDataSources().get(0).getFrom());
  }

  @Test
  void shouldReturnDefaultInstanceAndHoldingRules_whenMappingProfileTransformationsIsEmpty_HoldingAndInstanceRecordTypes() {
    // given
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withRecordTypes(Arrays.asList(HOLDINGS,INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(2, rules.size());
    assertEquals(DEFAULT_RULE_FIELD_VALUE, rules.get(0).getField());
    assertEquals(DEFAULT_RULE_DESCRIPTION, rules.get(0).getDescription());
    assertEquals(DEFAULT_RULE_FROM_VALUE, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(DEFAULT_HOLDING_RULE_FIELD_VALUE, rules.get(1).getField());
    assertEquals(DEFAULT_HOLDING_RULE_DESCRIPTION, rules.get(1).getDescription());
    assertEquals(DEFAULT_HOLDING_RULE_FROM_VALUE, rules.get(1).getDataSources().get(0).getFrom());
  }

  @Test
  void shouldReturnEmptyRules_whenMappingProfileTransformationsIsNotEnabled() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(false)
      .withRecordType(INSTANCE);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(ImmutableList.of(transformations))
      .withRecordTypes(singletonList(INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldReturnEmptyRules_whenMappingProfileTransformationsPathIsEmpty() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(EMPTY)
      .withRecordType(HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(ImmutableList.of(transformations))
      .withRecordTypes(singletonList(INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldReturnEmptyRules_whenMappingProfileTransformationsFieldIdIsEmpty() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withTransformation(EMPTY)
      .withRecordType(INSTANCE)
      .withFieldId(EMPTY);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(ImmutableList.of(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldReturnDefaultRule_whenTransformationsValueIsEmpty_andTransformationIdEqualsDefaultRuleId() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withFieldId(DEFAULT_RULE_ID)
      .withTransformation(EMPTY)
      .withRecordType(INSTANCE);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(ImmutableList.of(transformations))
      .withRecordTypes(singletonList(INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(DEFAULT_RULE_ID, rules.get(0).getId());
    assertEquals(DEFAULT_RULE_FIELD_VALUE, rules.get(0).getField());
    assertEquals(DEFAULT_RULE_DESCRIPTION, rules.get(0).getDescription());
    assertEquals(DEFAULT_RULE_FROM_VALUE, rules.get(0).getDataSources().get(0).getFrom());
  }

  @Test
  void shouldReturnRulesWithOneTransformationRule_whenMappingProfileTransformationsContainsValueWithoutSubfield() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withFieldId(FIELD_ID_1)
      .withRecordType(HOLDINGS)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(transformations));

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
    Transformations transformations1 = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withFieldId(FIELD_ID_1)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.ITEM);
    Transformations transformations2 = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_2)
      .withFieldId(FIELD_ID_2)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_2)
      .withRecordType(RecordType.ITEM);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(transformations1, transformations2));

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
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withFieldId(FIELD_ID_1)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_WITH_SUBFIELD)
      .withRecordType(RecordType.ITEM);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(transformations));

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
  void shouldReturnTransformationRuleWithPermanentLocationTranslation() {
    // given
    Transformations permanentLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(permanentLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(PERMANENT_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldNotReturnTransformationRuleWithPermanentLocationTranslation_whenPermanentLocationEqualsTemporaryLocation() {
    // given
    Transformations permanentLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(TEMPORARY_LOCATION_FIELD_ID)
      .withPath(TEMPORARY_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(permanentLocationTransformations, temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TEMPORARY_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnTransformationRuleWithTemporaryLocationTranslation() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(TEMPORARY_LOCATION_FIELD_ID)
      .withPath(TEMPORARY_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TEMPORARY_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnPermanentLocationRuleWithTranslationForCodeField() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_CODE_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

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
  void shouldReturnPermanentLocationRuleWithTranslationForLibraryName() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_LIBRARY_NAME_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

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
  void shouldReturnPermanentLocationRuleWithTranslationForLibraryCode() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_LIBRARY_CODE_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

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
  void shouldReturnTransformationRuleWithEffectiveLocationTranslation() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(EFFECTIVE_LOCATION_FIELD_ID)
      .withPath(EFFECTIVE_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.ITEM);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(EFFECTIVE_LOCATION_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(SET_LOCATION_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnPermanentLocationRuleWithTranslationForCampusName() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_CAMPUS_NAME_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

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
  void shouldReturnPermanentLocationRuleWithTranslationForCampusCode() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_CAMPUS_CODE_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

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
  void shouldReturnPermanentLocationRuleWithTranslationForInstitutionName() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_INSTITUTION_NAME_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

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
  void shouldReturnPermanentLocationRuleWithTranslationForInstitutionCode() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(PERMANENT_LOCATION_INSTITUTION_CODE_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

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
  void shouldReturnRuleWithDefaultTranslationWhenFieldIdContainsOneWord() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(ONE_WORD_LOCATION_FIELD_ID)
      .withPath(PERMANENT_LOCATION_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.HOLDINGS);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

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
  void shouldReturnTransformationRuleWithMaterialTypeTranslation() {
    // given
    Transformations temporaryLocationTransformations = new Transformations()
      .withEnabled(true)
      .withFieldId(MATERIAL_TYPE_FIELD_ID)
      .withPath(MATERIAL_TYPE_PATH)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(RecordType.ITEM);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(temporaryLocationTransformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(MATERIAL_TYPE_PATH, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(SET_MATERIAL_TYPE_FUNCTION, rules.get(0).getDataSources().get(0).getTranslation().getFunction());
  }

  @Test
  void shouldReturnSingleRule_WhenTransformationHasMultipleSubFieldsWithSameFieldId() {
    // given
    Transformations transformation1 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_FIELD_ID)
      .withPath(CALLNUMBER_FIELD_PATH)
      .withTransformation("900ff$a")
      .withRecordType(RecordType.ITEM);
    Transformations transformation2 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_PREFIX_FIELD_ID)
      .withPath(CALLNUMBER_PREFIX_FIELD_PATH)
      .withTransformation("900ff$b")
      .withRecordType(RecordType.ITEM);
    Transformations transformation3 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_SUFFIX_FIELD_ID)
      .withPath(CALLNUMBER_SUFFIX_FIELD_PATH)
      .withTransformation("900ff$c")
      .withRecordType(RecordType.ITEM);
    List<Transformations> transformations = Lists.newArrayList(transformation1, transformation2, transformation3);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(transformations);
    //when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    //then
    List<Rule> ruleList = rules.stream().filter(rule -> rule.getField().equals("900")).collect(Collectors.toList());
    assertEquals(1, ruleList.size());
    //3 data sources for subfields $a, $b, $c and 2 for indicators
    assertEquals(5, ruleList.get(0).getDataSources().size());
  }

  @Test
  void shouldReturnSingleRule_WhenTransformationHasMultipleSubFieldsWithSameFieldIdDifferentIndicators() {
    // given
    Transformations transformation1 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_FIELD_ID)
      .withPath(CALLNUMBER_FIELD_PATH)
      .withTransformation("900ff$a")
      .withRecordType(RecordType.ITEM);
    Transformations transformation2 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_PREFIX_FIELD_ID)
      .withPath(CALLNUMBER_PREFIX_FIELD_PATH)
      .withTransformation("900  $b")
      .withRecordType(RecordType.ITEM);
    Transformations transformation3 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_SUFFIX_FIELD_ID)
      .withPath(CALLNUMBER_SUFFIX_FIELD_PATH)
      .withTransformation("90011$c")
      .withRecordType(RecordType.ITEM);
    List<Transformations> transformations = Lists.newArrayList(transformation1, transformation2, transformation3);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(transformations);
    //when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    //then
    List<Rule> ruleList = rules.stream().filter(rule -> rule.getField().equals("900")).collect(Collectors.toList());
    assertEquals(1, ruleList.size());
    //3 data sources for subfields $a, $b, $c and 2 for indicators
    assertEquals(5, ruleList.get(0).getDataSources().size());
    //the first field's indicators are used
    assertEquals(2, ruleList.get(0).getDataSources().stream().
      filter(ds -> ds.getIndicator() != null && ds.getTranslation().getParameter("value").equals("f")).count());
  }

  @Test
  void shouldReturnCombinedRule_whenTransformationIsEmpty_andDefaultRuleHasMultipleSubfields() {
    // given
    List<Rule> defaultRules = setUpElectorincAccesDefaultRuleWithIdicators();
    doReturn(defaultRules).when(ruleFactory).getDefaultRulesFromFile();
    Transformations transformation = new Transformations()
      .withEnabled(true)
      .withFieldId("instance.electronic.access.linktext.related.resource")
      .withPath("$.instance.electronicAccess[?(@.relationshipId=='5bfe1b7b-f151-4501-8cfa-23b321d5cd1e')].linkText")
      .withTransformation(EMPTY)
      .withRecordType(INSTANCE);
    List<Transformations> transformations = Lists.newArrayList(transformation);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(transformations);

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
    Transformations transformation = new Transformations()
      .withEnabled(true)
      .withFieldId("instance.electronic.access.linktext.related.resource")
      .withPath("$.instance.electronicAccess[?(@.relationshipId=='5bfe1b7b-f151-4501-8cfa-23b321d5cd1e')].linkText")
      .withTransformation(EMPTY)
      .withRecordType(INSTANCE);
    List<Transformations> transformations = Lists.newArrayList(transformation);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(transformations);

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldNotReturnCombinedRule_whenEmptyTransformationFieldDoesntMatchDefaultSubfieldRuleId() {
    // given
    Transformations transformation = new Transformations()
      .withEnabled(true)
      .withFieldId("instance.electronic.access.nonexistingsubfield")
      .withPath("$.instance.electronicAccess.nonexistingsubfield")
      .withTransformation(EMPTY)
      .withRecordType(INSTANCE);
    List<Transformations> transformations = Lists.newArrayList(transformation);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(transformations);

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(0, rules.size());
  }

  @Test
  void shouldReturnDefaultRuleWithHoldingsAndItemRules_whenMappingProfileIsDefault_andContainsHoldingsAndItemTransformations() {
    // given
    Transformations holdingsTransformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withFieldId(FIELD_ID_1)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1)
      .withRecordType(HOLDINGS);
    Transformations itemTransformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_2)
      .withFieldId(FIELD_ID_2)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_2)
      .withRecordType(ITEM);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(DEFAULT_MAPPING_PROFILE_ID)
      .withTransformations(ImmutableList.of(holdingsTransformations, itemTransformations))
      .withRecordTypes(ImmutableList.of(INSTANCE, HOLDINGS, ITEM));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(3, rules.size());
    assertEquals(TRANSFORMATION_FIELD_VALUE_1, rules.get(0).getField());
    assertEquals(TRANSFORMATIONS_PATH_1, rules.get(0).getDataSources().get(0).getFrom());
    assertEquals(TRANSFORMATION_FIELD_VALUE_2, rules.get(1).getField());
    assertEquals(TRANSFORMATIONS_PATH_2, rules.get(1).getDataSources().get(0).getFrom());
    assertEquals(DEFAULT_RULE_ID, rules.get(2).getId());
    assertEquals(DEFAULT_RULE_FIELD_VALUE, rules.get(2).getField());
    assertEquals(DEFAULT_RULE_DESCRIPTION, rules.get(2).getDescription());
    assertEquals(DEFAULT_RULE_FROM_VALUE, rules.get(2).getDataSources().get(0).getFrom());
  }

  private void setUpDefaultRules() {
    DataSource dataSource = new DataSource();
    dataSource.setFrom(DEFAULT_RULE_FROM_VALUE);
    Rule defaultRule = new Rule();
    defaultRule.setId(DEFAULT_RULE_ID);
    defaultRule.setField(DEFAULT_RULE_FIELD_VALUE);
    defaultRule.setDescription(DEFAULT_RULE_DESCRIPTION);
    defaultRule.setDataSources(Lists.newArrayList(dataSource));
    defaultInstanceRules = Lists.newArrayList(defaultRule);
    DataSource holdingDataSource = new DataSource();
    holdingDataSource.setFrom(DEFAULT_HOLDING_RULE_FROM_VALUE);
    Rule holdingDefaultRule = new Rule();
    holdingDefaultRule.setId(DEFAULT_HOLDING_RULE_ID);
    holdingDefaultRule.setField(DEFAULT_HOLDING_RULE_FIELD_VALUE);
    holdingDefaultRule.setDescription(DEFAULT_HOLDING_RULE_DESCRIPTION);
    holdingDefaultRule.setDataSources(Lists.newArrayList(holdingDataSource));
    defaultHoldingsRules = Lists.newArrayList(holdingDefaultRule);
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
