package org.folio.dataexp.service.export.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import static org.mockito.Mockito.verify;
import java.util.Collections;
import java.util.Set;
import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class RuleFactoryTest {

  @InjectMocks private RuleFactory ruleFactory;

  @Test
  @TestMate(name = "TestMate-d999471200a03a6a5b0d63efe04dbe53")
  void shouldReturnEmpty_whenRecordTypeIsNotInstance() throws TransformationRuleException {
    // Given
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setFieldId("holdings.hrid");
    transformations.setRecordType(RecordTypes.HOLDINGS);
    List<Rule> defaultRules = new ArrayList<>();
    // When
    Optional<Rule> result =
        ruleFactory.createDefaultByTransformations(transformations, defaultRules);
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @TestMate(name = "TestMate-3dcc1f62c61e104028cf0b12117a1d11")
  void testCreateDefaultByTransformationsShouldPropagateTransformationRuleException() {
    // Given
    // We use a fieldId that contains "transformation.builder" to trigger the
    // TransformationRuleBuilder
    // which is already instantiated in the static map of the RuleFactory.
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setFieldId("transformation.builder.test");
    transformations.setTransformation("900  $a"); // Valid transformation format
    transformations.setRecordType(RecordTypes.INSTANCE);

    // TransformationRuleBuilder.build iterates over the provided rules.
    // According to Reference 5, it throws TransformationRuleException if an existing rule
    // for the same field has null indicators.
    List<Rule> defaultRules = new ArrayList<>();
    Rule existingRule = new Rule();
    existingRule.setField("900");
    existingRule.setIndicators(null); // This triggers the exception in TransformationRuleBuilder
    defaultRules.add(existingRule);
    // When & Then
    // The method should delegate to TransformationRuleBuilder, which throws the exception,
    // and RuleFactory should propagate it.
    assertThrows(
        TransformationRuleException.class,
        () -> ruleFactory.createDefaultByTransformations(transformations, defaultRules));
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-bc550a9f3396180eab1c73ead90e3ebb")
  @CsvSource({"false, instance.title", "true, ''", "true, "})
  void createDefaultByTransformations_shouldReturnEmpty_whenDisabledOrFieldIdBlank(
      boolean enabled, String fieldId) throws TransformationRuleException {
    // Given
    Transformations transformations = new Transformations();
    transformations.setEnabled(enabled);
    transformations.setFieldId(fieldId);
    transformations.setRecordType(RecordTypes.INSTANCE);
    List<Rule> defaultRules = new ArrayList<>();
    // When
    Optional<Rule> result =
        ruleFactory.createDefaultByTransformations(transformations, defaultRules);
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @TestMate(name = "TestMate-8731170170f755ee33961c2944bda653")
  void createDefaultByTransformations_shouldUseSpecificBuilder_whenFieldIdMatchesKey()
      throws TransformationRuleException {

    // Given
    var transformationFieldId = "instance.electronic.access.uri";
    var transformationPath = "$.source.uri";
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setFieldId(transformationFieldId);
    transformations.setPath(transformationPath);
    transformations.setRecordType(RecordTypes.INSTANCE);

    var defaultRuleMarcField = "856";
    Rule defaultRule = new Rule();
    defaultRule.setId("instance.electronic.access");
    defaultRule.setField(defaultRuleMarcField);

    var expectedSubfield = "u";
    DataSource matchingDataSource = new DataSource();
    matchingDataSource.setFrom("$.instance.electronicAccess[*].uri");
    matchingDataSource.setSubfield(expectedSubfield);
    defaultRule.setDataSources(new ArrayList<>(List.of(matchingDataSource)));
    List<Rule> defaultRules = new ArrayList<>(List.of(defaultRule));
    // When
    Optional<Rule> result =
        ruleFactory.createDefaultByTransformations(transformations, defaultRules);
    // Then
    assertTrue(result.isPresent());
    Rule actualRule = result.get();
    assertThat(actualRule.getField()).isEqualTo(defaultRuleMarcField);

    DataSource actualDataSource =
        actualRule.getDataSources().stream()
            .filter(ds -> ds.getIndicator() == null)
            .findFirst()
            .orElseThrow();

    assertThat(actualDataSource.getFrom()).isEqualTo(transformationPath);
    assertThat(actualDataSource.getSubfield()).isEqualTo(expectedSubfield);
  }

  @Test
  @TestMate(name = "TestMate-4959e1b60d69a04af56ac35d2a056652")
  void createDefaultByTransformations_shouldFallbackToDefaultBuilder_whenNoSpecificKeyMatches()
      throws TransformationRuleException {

    // Given
    var fieldId = "instance.title";
    Transformations transformations = new Transformations();
    transformations.setEnabled(true);
    transformations.setFieldId(fieldId);
    transformations.setRecordType(RecordTypes.INSTANCE);
    var marcField = "245";
    Rule titleRule = new Rule();
    titleRule.setId(fieldId);
    titleRule.setField(marcField);
    List<Rule> defaultRules = new ArrayList<>(List.of(titleRule));
    // When
    Optional<Rule> result =
        ruleFactory.createDefaultByTransformations(transformations, defaultRules);
    // Then
    assertTrue(result.isPresent());
    Rule actualRule = result.get();
    assertThat(actualRule.getId()).isEqualTo(fieldId);
    assertThat(actualRule.getField()).isEqualTo(marcField);
  }

  @Test
  @TestMate(name = "TestMate-7e6135e04aa1ae0d5457e4de2d286332")
  void getRulesShouldReturnAllRulesWhenNoSuppressionIsActive() throws TransformationRuleException {
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setSuppress999ff(false);
    mappingProfile.setFieldsSuppression(null);

    var rule001 = new Rule();
    rule001.setField("001");
    var rule999ff = new Rule();
    rule999ff.setField("999");

    var translationF = new Translation();
    translationF.setParameters(Map.of("value", "f"));

    var ds1 = new DataSource();
    ds1.setIndicator("1");
    ds1.setTranslation(translationF);

    var ds2 = new DataSource();
    ds2.setIndicator("2");
    ds2.setTranslation(translationF);

    rule999ff.setDataSources(List.of(ds1, ds2));
    var rule100 = new Rule();
    rule100.setField("100");

    var expectedRules = List.of(rule001, rule999ff, rule100);

    // Create a spy of the injected ruleFactory to allow partial mocking of buildRules
    var spyRuleFactory = org.mockito.Mockito.spy(ruleFactory);
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
    var mappingProfile = new MappingProfile();
    mappingProfile.setSuppress999ff(true);
    mappingProfile.setFieldsSuppression(null);
    var translationF = new Translation();
    translationF.setParameters(Map.of("value", "f"));
    var ds1 = new DataSource();
    ds1.setIndicator("1");
    ds1.setTranslation(translationF);
    var ds2 = new DataSource();
    ds2.setIndicator("2");
    ds2.setTranslation(translationF);
    var rule999ff = new Rule();
    rule999ff.setField("999");
    rule999ff.setDataSources(List.of(ds1, ds2));
    var translationA = new Translation();
    translationA.setParameters(Map.of("value", "a"));
    var translationB = new Translation();
    translationB.setParameters(Map.of("value", "b"));
    var ds3 = new DataSource();
    ds3.setIndicator("1");
    ds3.setTranslation(translationA);
    var ds4 = new DataSource();
    ds4.setIndicator("2");
    ds4.setTranslation(translationB);
    var rule999ab = new Rule();
    rule999ab.setField("999");
    rule999ab.setDataSources(List.of(ds3, ds4));
    var rule001 = new Rule();
    rule001.setField("001");
    rule001.setDataSources(new ArrayList<>());
    var initialRules = List.of(rule999ff, rule999ab, rule001);
    // Create a spy of the ruleFactory to allow partial mocking of the buildRules method
    var spyRuleFactory = org.mockito.Mockito.spy(ruleFactory);
    doReturn(initialRules).when(spyRuleFactory).buildRules(any(MappingProfile.class));
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
    var mappingProfile = new MappingProfile();
    mappingProfile.setSuppress999ff(false);
    mappingProfile.setFieldsSuppression(" 500, 700 ");
    var rule100 = new Rule();
    rule100.setField("100");
    var rule500 = new Rule();
    rule500.setField("500");
    var rule700 = new Rule();
    rule700.setField("700");
    var initialRules = List.of(rule100, rule500, rule700);
    // Create a spy of the injected ruleFactory to allow partial mocking of the internal buildRules
    // method
    var spyRuleFactory = org.mockito.Mockito.spy(ruleFactory);
    doReturn(initialRules).when(spyRuleFactory).buildRules(any(MappingProfile.class));
    // When
    var actualRules = spyRuleFactory.getRules(mappingProfile);
    // Then
    assertThat(actualRules).hasSize(1).containsExactly(rule100).doesNotContain(rule500, rule700);
  }

  @Test
  @TestMate(name = "TestMate-93134a9d3263b79be86f19751c59449b")
  void getRulesShouldApplyBoth999ffAndFieldsSuppressionSimultaneously() throws Exception {
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setSuppress999ff(true);
    mappingProfile.setFieldsSuppression("100");
    var translationF = new Translation();
    translationF.setParameters(Map.of("value", "f"));
    var ds1 = new DataSource();
    ds1.setIndicator("1");
    ds1.setTranslation(translationF);
    var ds2 = new DataSource();
    ds2.setIndicator("2");
    ds2.setTranslation(translationF);
    var rule999ff = new Rule();
    rule999ff.setField("999");
    rule999ff.setDataSources(List.of(ds1, ds2));
    var rule100 = new Rule();
    rule100.setField("100");
    var rule200 = new Rule();
    rule200.setField("200");
    var initialRules = List.of(rule999ff, rule100, rule200);
    var spyRuleFactory = spy(ruleFactory);
    doReturn(initialRules).when(spyRuleFactory).buildRules(any(MappingProfile.class));
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
    var mappingProfile = new MappingProfile();
    mappingProfile.setSuppress999ff(false);
    mappingProfile.setFieldsSuppression(" , , ");
    var rule100 = new Rule();
    rule100.setField("100");
    var rule245 = new Rule();
    rule245.setField("245");
    var initialRules = List.of(rule100, rule245);
    var spyRuleFactory = spy(ruleFactory);
    doReturn(initialRules).when(spyRuleFactory).buildRules(any(MappingProfile.class));
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
    var holdingsRule = new Rule();
    holdingsRule.setId("holdings.hrid");
    holdingsRule.setField("001");
    var defaultRulesFromConfigFile = new ArrayList<Rule>();
    var defaultHoldingsRulesFromConfigFile = List.of(holdingsRule);
    var ruleFactoryLocal =
        new RuleFactory(defaultRulesFromConfigFile, defaultHoldingsRulesFromConfigFile);
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
    var defaultRule = new Rule();
    defaultRule.setId("instance.hrid");
    defaultRule.setField("001");
    var defaultRulesFromConfigFile = List.of(defaultRule);
    var defaultHoldingsRulesFromConfigFile = new ArrayList<Rule>();
    var ruleFactoryLocal =
        new RuleFactory(defaultRulesFromConfigFile, defaultHoldingsRulesFromConfigFile);
    // When
    var actualRules = ruleFactoryLocal.buildRules(null);
    // Then
    assertThat(actualRules).containsExactly(defaultRule);
  }

  @Test
  @TestMate(name = "TestMate-ff3b8fa3d9aa0238868a7fedc302cefe")
  void buildRulesShouldCallCreateWhenRulesFromConfigIsEmpty() throws TransformationRuleException {
    // Given
    var instanceDefaultRule = new Rule();
    instanceDefaultRule.setId("instance.default");
    instanceDefaultRule.setField("001");
    var holdingsDefaultRule = new Rule();
    holdingsDefaultRule.setId("holdings.default");
    holdingsDefaultRule.setField("002");
    var defaultRulesFromConfigFile = List.of(instanceDefaultRule);
    var defaultHoldingsRulesFromConfigFile = List.of(holdingsDefaultRule);
    var ruleFactoryWithDefaults =
        new RuleFactory(defaultRulesFromConfigFile, defaultHoldingsRulesFromConfigFile);
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString("f3f00482-936d-470a-819a-9769db382793"));
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE));
    mappingProfile.setTransformations(new ArrayList<>());
    // When
    var actualRules = ruleFactoryWithDefaults.buildRules(mappingProfile);
    // Then
    assertThat(actualRules)
        .hasSize(1)
        .containsExactly(instanceDefaultRule)
        .doesNotContain(holdingsDefaultRule);
  }

    @Test
  void createShouldAppendHoldingsDefaultRulesWhenRequested() throws TransformationRuleException {
    // TestMate-2dc55c8cc79b1f8dfcde3801e9b672a5
    // Given
    var initialRule = new Rule();
    initialRule.setId("instance.hrid");
    var initialRules = new ArrayList<>(List.of(initialRule));
    var defaultHoldingsRule = new Rule();
    defaultHoldingsRule.setId("holdings.default");
    var defaultHoldingsRules = List.of(defaultHoldingsRule);
    var ruleFactoryLocal = new RuleFactory(new ArrayList<>(), defaultHoldingsRules);
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString("c0a80101-0000-0000-0000-000000000001"));
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS));
    mappingProfile.setTransformations(new ArrayList<>());
    // When
    var actualRules = ruleFactoryLocal.create(mappingProfile, initialRules, true);
    // Then
    assertThat(actualRules)
        .isSameAs(initialRules)
        .hasSize(2)
        .containsExactly(initialRule, defaultHoldingsRule);
  }

    @Test
  void createShouldGenerateRulesFromTransformations() throws TransformationRuleException {
    // TestMate-b230324ed45cb21c127310ac69ed7026
    // Given
    var customProfileId = UUID.fromString("d0a80101-0000-0000-0000-000000000001");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(customProfileId);
    var transformation = new Transformations();
    transformation.setFieldId("instance.title");
    var transformations = List.of(transformation);
    mappingProfile.setTransformations(transformations);
    mappingProfile.setRecordTypes(Collections.emptyList());
    var initialDefaultRules = new ArrayList<Rule>();
    var expectedRule = new Rule();
    expectedRule.setId("transformed.rule");
    var expectedRulesSet = Set.of(expectedRule);
    var spyRuleFactory = spy(ruleFactory);
    doReturn(expectedRulesSet).when(spyRuleFactory).createByTransformations(transformations, initialDefaultRules);
    // When
    var actualRules = spyRuleFactory.create(mappingProfile, initialDefaultRules, false);
    // Then
    assertThat(actualRules)
        .hasSize(1)
        .containsExactly(expectedRule);
    verify(spyRuleFactory).createByTransformations(transformations, initialDefaultRules);
  }

    @Test
  void createShouldAppendDefaultInstanceRulesForDefaultProfile() throws TransformationRuleException {
    // TestMate-56278d4dda4f2cbc4d2effc83b633b69
    // Given
    var baselineRule = new Rule();
    baselineRule.setId("baseline.rule");
    var defaultRulesFromConfigFile = List.of(baselineRule);
    var ruleFactoryLocal = new RuleFactory(defaultRulesFromConfigFile, new ArrayList<>());
    var spyRuleFactory = spy(ruleFactoryLocal);
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString(DEFAULT_INSTANCE_MAPPING_PROFILE_ID));
    var transformations = List.of(new Transformations());
    mappingProfile.setTransformations(transformations);
    var customRule = new Rule();
    customRule.setId("custom.rule");
    var customRulesSet = Set.of(customRule);
    // Fix: Use eq() matcher for the first argument because anyList() is used for the second argument
    doReturn(customRulesSet)
        .when(spyRuleFactory)
        .createByTransformations(eq(transformations), anyList());
    // When
    var actualRules = spyRuleFactory.create(mappingProfile, new ArrayList<>(), false);
    // Then
    assertThat(actualRules).hasSize(2).containsExactly(customRule, baselineRule);
    verify(spyRuleFactory).createByTransformations(eq(transformations), anyList());
  }

    @Test
  void createShouldNotAppendHoldingsRulesWhenRequestedButTypeNotHoldings() throws TransformationRuleException {
    // TestMate-4f84bc23a2303857eb884a745670ae71
    // Given
    var initialRule = new Rule();
    initialRule.setId("instance.hrid");
    var initialRules = new ArrayList<>(List.of(initialRule));
    var defaultHoldingsRule = new Rule();
    defaultHoldingsRule.setId("holdings.default");
    var defaultHoldingsRules = List.of(defaultHoldingsRule);
    var ruleFactoryLocal = new RuleFactory(new ArrayList<>(), defaultHoldingsRules);
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString("d0a80101-0000-0000-0000-000000000001"));
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE));
    mappingProfile.setTransformations(null);
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
  void createShouldPropagateTransformationRuleExceptionFromCreateByTransformations() throws TransformationRuleException {
    // TestMate-25f02f01c02973ad5f8aa00674140688
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString("c0a80101-0000-0000-0000-000000000001"));
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE));
    var transformations = List.of(new Transformations());
    mappingProfile.setTransformations(transformations);
    var defaultRules = new ArrayList<Rule>();
    var exceptionMessage = "Transformation failed";
    var spyRuleFactory = spy(ruleFactory);
    doThrow(new TransformationRuleException(exceptionMessage))
        .when(spyRuleFactory)
        .createByTransformations(anyList(), anyList());
    // When
    var exception = assertThrows(TransformationRuleException.class, () ->
        spyRuleFactory.create(mappingProfile, defaultRules, false));
    // Then
    assertThat(exception.getMessage()).isEqualTo(exceptionMessage);
    verify(spyRuleFactory).createByTransformations(transformations, defaultRules);
  }
}
