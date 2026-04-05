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
    //
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
    //
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
}
