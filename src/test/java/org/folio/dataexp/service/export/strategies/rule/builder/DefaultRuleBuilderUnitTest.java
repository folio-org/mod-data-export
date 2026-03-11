package org.folio.dataexp.service.export.strategies.rule.builder;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Optional;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class DefaultRuleBuilderUnitTest {

    @Test
void testBuildShouldReturnMatchingRuleWhenFieldIdExists() {
    // TestMate-6b9624d6bbbcdf66559df82431df41d7
    // Given
    DefaultRuleBuilder ruleBuilder = new DefaultRuleBuilder();
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
    // When
    Optional<Rule> actualRuleOptional = ruleBuilder.build(rules, mappingTransformation);
    // Then
    assertThat(actualRuleOptional).isPresent();
    assertThat(actualRuleOptional.get()).isSameAs(targetRule);
}

    @Test
void testBuildShouldReturnEmptyOptionalWhenNoMatchingRuleFound() {
    // TestMate-a807189922cff440cac1970017193879
    // Given
    DefaultRuleBuilder ruleBuilder = new DefaultRuleBuilder();
    String nonExistentFieldId = "non-existent-id";
    Rule rule1 = new Rule();
    rule1.setId("rule-id-1");
    Rule rule2 = new Rule();
    rule2.setId("rule-id-2");
    List<Rule> rules = List.of(rule1, rule2);
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(nonExistentFieldId);
    // When
    Optional<Rule> result = ruleBuilder.build(rules, mappingTransformation);
    // Then
    assertThat(result).isEmpty();
}

    @Test
void testBuildShouldReturnEmptyOptionalWhenFieldIdIsNull() {
    // TestMate-fc371263a6198b15f75f0aa3b46c2c5c
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
void testBuildShouldReturnEmptyOptionalWhenRulesCollectionIsEmpty() {
    // TestMate-3006d18d3c7b38147f92b8b62dc1b75c
    // Given
    DefaultRuleBuilder ruleBuilder = new DefaultRuleBuilder();
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId("any-field-id");
    // When
    Optional<Rule> result = ruleBuilder.build(Collections.emptyList(), mappingTransformation);
    // Then
    assertThat(result).isEmpty();
}
}
