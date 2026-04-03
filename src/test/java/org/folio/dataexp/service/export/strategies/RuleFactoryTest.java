package org.folio.dataexp.service.export.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
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
    // TestMate-3dcc1f62c61e104028cf0b12117a1d11
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
}
