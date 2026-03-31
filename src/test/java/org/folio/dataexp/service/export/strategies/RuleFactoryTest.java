package org.folio.dataexp.service.export.strategies;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import org.folio.dataexp.service.export.strategies.rule.builder.DefaultRuleBuilder;
import org.mockito.MockedConstruction;

@ExtendWith(MockitoExtension.class)
class RuleFactoryTest {

  @Mock private List<Rule> defaultRulesFromConfigFile;

  @Mock private List<Rule> defaultHoldingsRulesFromConfigFile;

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
  void testCreateDefaultByTransformationsShouldPropagateTransformationRuleException() {
    // TestMate-3dcc1f62c61e104028cf0b12117a1d11
    // Given
    // We use a fieldId that contains "transformation.builder" to trigger the TransformationRuleBuilder
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
    assertThrows(TransformationRuleException.class, () ->
        ruleFactory.createDefaultByTransformations(transformations, defaultRules));
  }
}
