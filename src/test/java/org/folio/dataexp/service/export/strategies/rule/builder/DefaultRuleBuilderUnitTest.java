package org.folio.dataexp.service.export.strategies.rule.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRuleBuilderUnitTest {

  @Test
  @TestMate(name = "TestMate-6b9624d6bbbcdf66559df82431df41d7")
  void testBuildShouldReturnMatchingRuleWhenFieldIdExists() {
    // Given
    String targetFieldId = "rule-id-2";
    Rule rule1 = new Rule();
    rule1.setId("rule-id-1");
    Rule targetRule = new Rule();
    targetRule.setId(targetFieldId);
    Rule rule3 = new Rule();
    rule3.setId("rule-id-3");
    List<Rule> rules = List.of(rule1, targetRule, rule3);
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(targetFieldId);
    DefaultRuleBuilder ruleBuilder = new DefaultRuleBuilder();
    // When
    Optional<Rule> actualRuleOptional = ruleBuilder.build(rules, mappingTransformation);
    // Then
    assertThat(actualRuleOptional).isPresent();
    assertThat(actualRuleOptional.get()).isSameAs(targetRule);
  }

  @Test
  @TestMate(name = "TestMate-a807189922cff440cac1970017193879")
  void testBuildShouldReturnEmptyOptionalWhenNoMatchingRuleFound() {
    // Given

    Rule rule1 = new Rule();
    rule1.setId("rule-id-1");
    Rule rule2 = new Rule();
    rule2.setId("rule-id-2");
    List<Rule> rules = List.of(rule1, rule2);
    DefaultRuleBuilder ruleBuilder = new DefaultRuleBuilder();
    String nonExistentFieldId = "non-existent-id";
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(nonExistentFieldId);
    // When
    Optional<Rule> result = ruleBuilder.build(rules, mappingTransformation);
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @TestMate(name = "TestMate-fc371263a6198b15f75f0aa3b46c2c5c")
  void testBuildShouldReturnEmptyOptionalWhenFieldIdIsNull() {
    // Given
    DefaultRuleBuilder ruleBuilder = new DefaultRuleBuilder();
    Rule rule1 = new Rule();
    rule1.setId("rule-id-1");
    List<Rule> rules = List.of(rule1);
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(null);
    // When
    Optional<Rule> result = ruleBuilder.build(rules, mappingTransformation);
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @TestMate(name = "TestMate-3006d18d3c7b38147f92b8b62dc1b75c")
  void testBuildShouldReturnEmptyOptionalWhenRulesCollectionIsEmpty() {
    // Given
    DefaultRuleBuilder ruleBuilder = new DefaultRuleBuilder();
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId("any-field-id");
    // When
    Optional<Rule> result = ruleBuilder.build(Collections.emptyList(), mappingTransformation);
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @TestMate(name = "TestMate-9245c9bb38f312c03e14dbca44993299")
  void testBuildShouldIgnoreRulesWithNullIds() {
    // Given
    DefaultRuleBuilder ruleBuilder = new DefaultRuleBuilder();
    String targetFieldId = "target-id";
    Rule ruleWithNullId = new Rule();
    ruleWithNullId.setId(null);
    Rule targetRule = new Rule();
    targetRule.setId(targetFieldId);
    List<Rule> rules = List.of(ruleWithNullId, targetRule);
    // When
    Optional<Rule> actualRuleOptional = ruleBuilder.build(rules, targetFieldId);
    // Then
    assertThat(actualRuleOptional).isPresent().containsSame(targetRule);
  }
}
