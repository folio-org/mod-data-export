package org.folio.dataexp.service.export.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.folio.processor.translations.Translation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleFactoryTest {

  @InjectMocks private RuleFactory ruleFactory;

  // ===== Helper methods =====

  private static Transformations createTransformations(
      boolean enabled, String fieldId, RecordTypes recordType) {
    var t = new Transformations();
    t.setEnabled(enabled);
    t.setFieldId(fieldId);
    t.setRecordType(recordType);
    return t;
  }

  private static Rule createRule(String id, String field) {
    var rule = new Rule();
    rule.setId(id);
    rule.setField(field);
    return rule;
  }

  private static DataSource createDataSource(String indicator, String translationValue) {
    var ds = new DataSource();
    ds.setIndicator(indicator);
    if (translationValue != null) {
      ds.setTranslation(createTranslation(translationValue));
    }
    return ds;
  }

  private static Translation createTranslation(String value) {
    var translation = new Translation();
    translation.setParameters(Map.of("value", value));
    return translation;
  }

  private static Rule create999ffRule() {
    var rule = new Rule();
    rule.setField("999");
    rule.setDataSources(List.of(createDataSource("1", "f"), createDataSource("2", "f")));
    return rule;
  }

  private static MappingProfile createMappingProfile(
      UUID id, boolean suppress999ff, String fieldsSuppression, List<RecordTypes> recordTypes) {
    var profile = new MappingProfile();
    profile.setId(id);
    profile.setSuppress999ff(suppress999ff);
    profile.setFieldsSuppression(fieldsSuppression);
    if (recordTypes != null) {
      profile.setRecordTypes(recordTypes);
    }
    return profile;
  }

  // ===== Tests =====

  @Test
  @TestMate(name = "TestMate-d999471200a03a6a5b0d63efe04dbe53")
  void shouldReturnEmpty_whenRecordTypeIsNotInstance() throws TransformationRuleException {
    // Given
    var transformations = createTransformations(true, "holdings.hrid", RecordTypes.HOLDINGS);

    // When
    Optional<Rule> result =
        ruleFactory.createDefaultByTransformations(transformations, new ArrayList<>());

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @TestMate(name = "TestMate-3dcc1f62c61e104028cf0b12117a1d11")
  void testCreateDefaultByTransformationsShouldPropagateTransformationRuleException() {
    // Given
    var transformations =
        createTransformations(true, "transformation.builder.test", RecordTypes.INSTANCE);
    transformations.setTransformation("900  $a");

    var existingRule = new Rule();
    existingRule.setField("900");
    existingRule.setIndicators(null);
    var defaultRules = new ArrayList<>(List.of(existingRule));

    // When & Then
    assertThatThrownBy(
            () -> ruleFactory.createDefaultByTransformations(transformations, defaultRules))
        .isInstanceOf(TransformationRuleException.class);
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-bc550a9f3396180eab1c73ead90e3ebb")
  @CsvSource({"false, instance.title", "true, ''", "true, "})
  void createDefaultByTransformations_shouldReturnEmpty_whenDisabledOrFieldIdBlank(
      boolean enabled, String fieldId) throws TransformationRuleException {
    // Given
    var transformations = createTransformations(enabled, fieldId, RecordTypes.INSTANCE);

    // When
    Optional<Rule> result =
        ruleFactory.createDefaultByTransformations(transformations, new ArrayList<>());

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @TestMate(name = "TestMate-8731170170f755ee33961c2944bda653")
  void createDefaultByTransformations_shouldUseSpecificBuilder_whenFieldIdMatchesKey()
      throws TransformationRuleException {
    // Given
    var transformations =
        createTransformations(true, "instance.electronic.access.uri", RecordTypes.INSTANCE);
    transformations.setPath("$.source.uri");

    var matchingDataSource = new DataSource();
    matchingDataSource.setFrom("$.instance.electronicAccess[*].uri");
    matchingDataSource.setSubfield("u");

    var defaultRule = createRule("instance.electronic.access", "856");
    defaultRule.setDataSources(new ArrayList<>(List.of(matchingDataSource)));

    // When
    Optional<Rule> result =
        ruleFactory.createDefaultByTransformations(
            transformations, new ArrayList<>(List.of(defaultRule)));

    // Then
    assertThat(result).isPresent();
    var actualRule = result.get();
    assertThat(actualRule.getField()).isEqualTo("856");

    var actualDataSource =
        actualRule.getDataSources().stream()
            .filter(ds -> ds.getIndicator() == null)
            .findFirst()
            .orElseThrow();
    assertThat(actualDataSource.getFrom()).isEqualTo("$.source.uri");
    assertThat(actualDataSource.getSubfield()).isEqualTo("u");
  }

  @Test
  @TestMate(name = "TestMate-4959e1b60d69a04af56ac35d2a056652")
  void createDefaultByTransformations_shouldFallbackToDefaultBuilder_whenNoSpecificKeyMatches()
      throws TransformationRuleException {
    // Given
    var transformations = createTransformations(true, "instance.title", RecordTypes.INSTANCE);
    var titleRule = createRule("instance.title", "245");

    // When
    Optional<Rule> result =
        ruleFactory.createDefaultByTransformations(
            transformations, new ArrayList<>(List.of(titleRule)));

    // Then
    assertThat(result)
        .isPresent()
        .get()
        .satisfies(
            rule -> {
              assertThat(rule.getId()).isEqualTo("instance.title");
              assertThat(rule.getField()).isEqualTo("245");
            });
  }

  @Test
  @TestMate(name = "TestMate-7e6135e04aa1ae0d5457e4de2d286332")
  void getRulesShouldReturnAllRulesWhenNoSuppressionIsActive() throws TransformationRuleException {
    // Given
    var mappingProfile = createMappingProfile(null, false, null, null);
    var rule001 = createRule(null, "001");
    var rule999ff = create999ffRule();
    var rule100 = createRule(null, "100");
    var expectedRules = List.of(rule001, rule999ff, rule100);

    var spyRuleFactory = spy(ruleFactory);
    doReturn(expectedRules).when(spyRuleFactory).buildRules(any(MappingProfile.class));

    // When
    var actualRules = spyRuleFactory.getRules(mappingProfile);

    // Then
    assertThat(actualRules).hasSize(3).containsExactly(rule001, rule999ff, rule100);
  }

  @Test
  @TestMate(name = "TestMate-b94b4a5ac8d1a9ce863cfd71b32fae81")
  void getRulesShouldFilter999ffWhenSuppress999ffIsTrue() throws TransformationRuleException {
    // Given
    var rule001 = new Rule();
    rule001.setField("001");
    rule001.setDataSources(new ArrayList<>());

    var rule999ff = create999ffRule();

    var rule999ab = new Rule();
    rule999ab.setField("999");
    rule999ab.setDataSources(List.of(createDataSource("1", "a"), createDataSource("2", "b")));

    var spyRuleFactory = spy(ruleFactory);
    doReturn(List.of(rule999ff, rule999ab, rule001))
        .when(spyRuleFactory)
        .buildRules(any(MappingProfile.class));

    var mappingProfile = createMappingProfile(null, true, null, null);

    // When
    var actualRules = spyRuleFactory.getRules(mappingProfile);

    // Then
    assertThat(actualRules)
        .hasSize(2)
        .containsExactlyInAnyOrder(rule999ab, rule001)
        .doesNotContain(rule999ff);
  }

  @Test
  @TestMate(name = "TestMate-f7765c57a9febf00d15cf800bde8e339")
  void getRulesShouldFilterSpecificFieldsWhenFieldsSuppressionIsProvided()
      throws TransformationRuleException {
    // Given
    var mappingProfile = createMappingProfile(null, false, " 500, 700 ", null);
    var rule100 = createRule(null, "100");
    var rule500 = createRule(null, "500");
    var rule700 = createRule(null, "700");

    var spyRuleFactory = spy(ruleFactory);
    doReturn(List.of(rule100, rule500, rule700))
        .when(spyRuleFactory)
        .buildRules(any(MappingProfile.class));

    // When
    var actualRules = spyRuleFactory.getRules(mappingProfile);

    // Then
    assertThat(actualRules).hasSize(1).containsExactly(rule100).doesNotContain(rule500, rule700);
  }

  @Test
  @TestMate(name = "TestMate-93134a9d3263b79be86f19751c59449b")
  void getRulesShouldApplyBoth999ffAndFieldsSuppressionSimultaneously() throws Exception {
    // Given
    var mappingProfile = createMappingProfile(null, true, "100", null);
    var rule999ff = create999ffRule();
    var rule100 = createRule(null, "100");
    var rule200 = createRule(null, "200");

    var spyRuleFactory = spy(ruleFactory);
    doReturn(List.of(rule999ff, rule100, rule200))
        .when(spyRuleFactory)
        .buildRules(any(MappingProfile.class));

    // When
    var actualRules = spyRuleFactory.getRules(mappingProfile);

    // Then
    assertThat(actualRules).hasSize(1).containsExactly(rule200).doesNotContain(rule999ff, rule100);
  }

  @Test
  @TestMate(name = "TestMate-26fcc3a6bd98ea236e266e45d0107973")
  void getRulesShouldHandleEmptyFieldsSuppressionStringGracefully()
      throws TransformationRuleException {
    // Given
    var mappingProfile = createMappingProfile(null, false, " , , ", null);
    var rule100 = createRule(null, "100");
    var rule245 = createRule(null, "245");

    var spyRuleFactory = spy(ruleFactory);
    doReturn(List.of(rule100, rule245)).when(spyRuleFactory).buildRules(any(MappingProfile.class));

    // When
    var actualRules = spyRuleFactory.getRules(mappingProfile);

    // Then
    assertThat(actualRules).hasSize(2).containsExactly(rule100, rule245);
  }

  @Test
  @TestMate(name = "TestMate-2825de9a93a8b8b2c1d3cec49562079e")
  void buildRulesShouldCallCreateWhenMappingProfileDoesNotContainInstance()
      throws TransformationRuleException {
    // Given
    var holdingsRule = createRule("holdings.hrid", "001");
    var ruleFactoryLocal = new RuleFactory(new ArrayList<>(), List.of(holdingsRule));
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS));

    // When
    var actualRules = ruleFactoryLocal.buildRules(mappingProfile);

    // Then
    assertThat(actualRules).containsExactly(holdingsRule);
  }

  @Test
  @TestMate(name = "TestMate-60324df72f9fc56335d82b4890b76984")
  void buildRulesShouldCallCreateWhenMappingProfileIsNull() throws TransformationRuleException {
    // Given
    var defaultRule = createRule("instance.hrid", "001");
    var ruleFactoryLocal = new RuleFactory(List.of(defaultRule), new ArrayList<>());

    // When
    var actualRules = ruleFactoryLocal.buildRules(null);

    // Then
    assertThat(actualRules).containsExactly(defaultRule);
  }

  @Test
  @TestMate(name = "TestMate-ff3b8fa3d9aa0238868a7fedc302cefe")
  void buildRulesShouldCallCreateWhenRulesFromConfigIsEmpty() throws TransformationRuleException {
    // Given
    var instanceDefaultRule = createRule("instance.default", "001");
    var holdingsDefaultRule = createRule("holdings.default", "002");
    var ruleFactoryLocal =
        new RuleFactory(List.of(instanceDefaultRule), List.of(holdingsDefaultRule));

    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString("f3f00482-936d-470a-819a-9769db382793"));
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE));
    mappingProfile.setTransformations(new ArrayList<>());

    // When
    var actualRules = ruleFactoryLocal.buildRules(mappingProfile);

    // Then
    assertThat(actualRules)
        .hasSize(1)
        .containsExactly(instanceDefaultRule)
        .doesNotContain(holdingsDefaultRule);
  }

  @Test
  @TestMate(name = "TestMate-2dc55c8cc79b1f8dfcde3801e9b672a5")
  void createShouldAppendHoldingsDefaultRulesWhenRequested() throws TransformationRuleException {
    // Given
    var mappingProfile =
        createMappingProfile(
            UUID.fromString("c0a80101-0000-0000-0000-000000000001"),
            false,
            null,
            List.of(RecordTypes.HOLDINGS));
    mappingProfile.setTransformations(new ArrayList<>());

    var initialRule = createRule("instance.hrid", null);
    var initialRules = new ArrayList<>(List.of(initialRule));
    var defaultHoldingsRule = createRule("holdings.default", null);
    var ruleFactoryLocal = new RuleFactory(new ArrayList<>(), List.of(defaultHoldingsRule));

    // When
    var actualRules = ruleFactoryLocal.create(mappingProfile, initialRules, true);

    // Then
    assertThat(actualRules)
        .isSameAs(initialRules)
        .hasSize(2)
        .containsExactly(initialRule, defaultHoldingsRule);
  }

  @Test
  @TestMate(name = "TestMate-b230324ed45cb21c127310ac69ed7026")
  void createShouldGenerateRulesFromTransformations() throws TransformationRuleException {
    // Given
    var customProfileId = UUID.fromString("d0a80101-0000-0000-0000-000000000001");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(customProfileId);
    var transformation = new Transformations();
    transformation.setFieldId("instance.title");
    mappingProfile.setTransformations(List.of(transformation));
    mappingProfile.setRecordTypes(Collections.emptyList());

    var initialDefaultRules = new ArrayList<Rule>();
    var expectedRule = createRule("transformed.rule", null);

    var spyRuleFactory = spy(ruleFactory);
    doReturn(Set.of(expectedRule))
        .when(spyRuleFactory)
        .createByTransformations(mappingProfile.getTransformations(), initialDefaultRules);

    // When
    var actualRules = spyRuleFactory.create(mappingProfile, initialDefaultRules, false);

    // Then
    assertThat(actualRules).hasSize(1).containsExactly(expectedRule);
    verify(spyRuleFactory)
        .createByTransformations(mappingProfile.getTransformations(), initialDefaultRules);
  }

  @Test
  @TestMate(name = "TestMate-56278d4dda4f2cbc4d2effc83b633b69")
  void createShouldAppendDefaultInstanceRulesForDefaultProfile()
      throws TransformationRuleException {
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString(DEFAULT_INSTANCE_MAPPING_PROFILE_ID));
    var transformations = List.of(new Transformations());
    mappingProfile.setTransformations(transformations);

    var baselineRule = createRule("baseline.rule", null);
    var ruleFactoryLocal = new RuleFactory(List.of(baselineRule), new ArrayList<>());
    var spyRuleFactory = spy(ruleFactoryLocal);

    var customRule = createRule("custom.rule", null);
    doReturn(Set.of(customRule))
        .when(spyRuleFactory)
        .createByTransformations(eq(transformations), anyList());

    // When
    var actualRules = spyRuleFactory.create(mappingProfile, new ArrayList<>(), false);

    // Then
    assertThat(actualRules).hasSize(2).containsExactly(customRule, baselineRule);
    verify(spyRuleFactory).createByTransformations(eq(transformations), anyList());
  }

  @Test
  @TestMate(name = "TestMate-4f84bc23a2303857eb884a745670ae71")
  void createShouldNotAppendHoldingsRulesWhenRequestedButTypeNotHoldings()
      throws TransformationRuleException {
    // Given
    var mappingProfile =
        createMappingProfile(
            UUID.fromString("d0a80101-0000-0000-0000-000000000001"),
            false,
            null,
            List.of(RecordTypes.INSTANCE));
    mappingProfile.setTransformations(null);

    var initialRule = createRule("instance.hrid", null);
    var initialRules = new ArrayList<>(List.of(initialRule));
    var defaultHoldingsRule = createRule("holdings.default", null);
    var ruleFactoryLocal = new RuleFactory(new ArrayList<>(), List.of(defaultHoldingsRule));

    // When
    var actualRules = ruleFactoryLocal.create(mappingProfile, initialRules, true);

    // Then
    assertThat(actualRules)
        .isSameAs(initialRules)
        .hasSize(1)
        .containsExactly(initialRule)
        .doesNotContain(defaultHoldingsRule);
  }

  @Test
  @TestMate(name = "TestMate-25f02f01c02973ad5f8aa00674140688")
  void createShouldPropagateTransformationRuleExceptionFromCreateByTransformations()
      throws TransformationRuleException {
    // Given
    var mappingProfile =
        createMappingProfile(
            UUID.fromString("c0a80101-0000-0000-0000-000000000001"),
            false,
            null,
            List.of(RecordTypes.INSTANCE));
    var transformations = List.of(new Transformations());
    mappingProfile.setTransformations(transformations);

    var defaultRules = new ArrayList<Rule>();
    var spyRuleFactory = spy(ruleFactory);
    doThrow(new TransformationRuleException("Transformation failed"))
        .when(spyRuleFactory)
        .createByTransformations(anyList(), anyList());

    // When & Then
    assertThatThrownBy(() -> spyRuleFactory.create(mappingProfile, defaultRules, false))
        .isInstanceOf(TransformationRuleException.class)
        .hasMessage("Transformation failed");
    verify(spyRuleFactory).createByTransformations(transformations, defaultRules);
  }
}
