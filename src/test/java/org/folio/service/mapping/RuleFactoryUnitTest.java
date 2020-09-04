package org.folio.service.mapping;

import com.google.common.collect.ImmutableList;
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
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import static org.folio.util.ExternalPathResolver.CAMPUSES;
import static org.folio.util.ExternalPathResolver.INSTITUTIONS;
import static org.folio.util.ExternalPathResolver.LIBRARIES;
import static org.mockito.Mockito.doReturn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class RuleFactoryUnitTest {
  private static final String DEFAULT_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";
  private static final String DEFAULT_RULE_FIELD_VALUE = "001";
  private static final String DEFAULT_RULE_DESCRIPTION = "defaultRuleDescription";
  private static final String DEFAULT_RULE_FROM_VALUE = "defaultFromValue";
  private static final String FIELD_ID_1 = "fieldId1";
  private static final String FIELD_ID_2 = "fieldId2";
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

  private static List<Rule> defaultRules;

  @BeforeEach
  public void setUp() {
    setUpDefaultRules();
    doReturn(defaultRules).when(ruleFactory).getDefaultRulesFromFile();
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
      .withId(DEFAULT_MAPPING_PROFILE_ID);

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
      .withId(UUID.randomUUID().toString());

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(DEFAULT_RULE_FIELD_VALUE, rules.get(0).getField());
    assertEquals(DEFAULT_RULE_DESCRIPTION, rules.get(0).getDescription());
    assertEquals(DEFAULT_RULE_FROM_VALUE, rules.get(0).getDataSources().get(0).getFrom());
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
      .withRecordTypes(Collections.singletonList(INSTANCE));

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
      .withRecordTypes(Collections.singletonList(INSTANCE));

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
      .withFieldId(FIELD_ID_1)
      .withTransformation(EMPTY)
      .withRecordType(INSTANCE);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(ImmutableList.of(transformations))
      .withRecordTypes(Collections.singletonList(INSTANCE));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(1, rules.size());
    assertEquals(FIELD_ID_1, rules.get(0).getId());
    assertEquals(DEFAULT_RULE_FIELD_VALUE, rules.get(0).getField());
    assertEquals(DEFAULT_RULE_DESCRIPTION, rules.get(0).getDescription());
    assertEquals(DEFAULT_RULE_FROM_VALUE, rules.get(0).getDataSources().get(0).getFrom());
  }

  @Test
  void shouldReturnDefaultRulesWithOneTransformationRule_whenMappingProfileTransformationsContainsValueWithoutSubfield() {
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
  void shouldReturnDefaultRulesWithTwoTransformationRules_whenMappingProfileTransformationsContainsValueWithoutSubfield() {
    // given
    Transformations transformations1 = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withFieldId(FIELD_ID_1)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1);
    Transformations transformations2 = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_2)
      .withFieldId(FIELD_ID_2)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_2);
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
  void shouldReturnDefaultRulesWithOneTransformationRule_whenTransformationsValueWithSubfieldAndIndicators() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withFieldId(FIELD_ID_1)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_WITH_SUBFIELD);
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
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1);
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
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1);
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
      .withTransformation("900ff$a");
    Transformations transformation2 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_PREFIX_FIELD_ID)
      .withPath(CALLNUMBER_PREFIX_FIELD_PATH)
      .withTransformation("900ff$b");
    Transformations transformation3 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_SUFFIX_FIELD_ID)
      .withPath(CALLNUMBER_SUFFIX_FIELD_PATH)
      .withTransformation("900ff$c");
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
      .withTransformation("900ff$a");
    Transformations transformation2 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_PREFIX_FIELD_ID)
      .withPath(CALLNUMBER_PREFIX_FIELD_PATH)
      .withTransformation("900  $b");
    Transformations transformation3 = new Transformations()
      .withEnabled(true)
      .withFieldId(CALLNUMBER_SUFFIX_FIELD_ID)
      .withPath(CALLNUMBER_SUFFIX_FIELD_PATH)
      .withTransformation("90011$c");
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


  private void setUpDefaultRules() {
    DataSource dataSource = new DataSource();
    dataSource.setFrom(DEFAULT_RULE_FROM_VALUE);
    Rule defaultRule = new Rule();
    defaultRule.setId(FIELD_ID_1);
    defaultRule.setField(DEFAULT_RULE_FIELD_VALUE);
    defaultRule.setDescription(DEFAULT_RULE_DESCRIPTION);
    defaultRule.setDataSources(Lists.newArrayList(dataSource));
    defaultRules = Lists.newArrayList(defaultRule);
  }

}
