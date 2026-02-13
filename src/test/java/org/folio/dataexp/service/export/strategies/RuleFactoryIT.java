package org.folio.dataexp.service.export.strategies;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;
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
  void returnDefaultRules_whenMappingProfileIsNull() throws TransformationRuleException {
    // when
    List<Rule> rules = ruleFactory.create(null);

    // then
    assertEquals(defaultRulesFromConfigFile, rules);
  }

  @Test
  void returnDefaultRules_whenMappingProfileIsDefault() throws TransformationRuleException {
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
  void returnDefaultRules_whenMappingProfileTransformationsIsEmpty()
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
  void returnDefaultHoldingRules_whenMappingProfileTransformationsIsEmptyForHoldingRecordType()
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
  void returnDefaultRules_whenTransformationsEmptyForHoldingAndInstance()
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
  void returnEmptyRules_whenTransformationsDisabled() throws TransformationRuleException {
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
  void returnEmptyRules_whenTransformationPathIsEmpty() throws TransformationRuleException {
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
  void returnEmptyRules_whenTransformationFieldIdIsEmpty() throws TransformationRuleException {
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
  void returnDefaultRule_whenTransformationValueEmptyAndFieldIdMatchesDefault()
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
  @SneakyThrows
  void suppressListedFields() {
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
  void suppress999Field() {
    var mappingProfile =
        MappingProfile.builder()
            .recordTypes(Collections.singletonList(RecordTypes.INSTANCE))
            .suppress999ff(true)
            .build();

    assertTrue(defaultRulesFromConfigFile.stream().anyMatch(rule -> "999".equals(rule.getField())));

    var rules = ruleFactory.getRules(mappingProfile);

    assertTrue(rules.stream().noneMatch(rule -> "999".equals(rule.getField())));
  }
}
