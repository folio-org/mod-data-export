package org.folio.dataexp.service.export.strategies.rule.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.folio.processor.translations.Translation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CombinedRuleBuilderTest {

  @Test
  @TestMate(name = "TestMate-147dd2ffd2da942497c4e3b416a53011")
  void buildShouldReturnCombinedRuleWhenDefaultRuleAndSubfieldDataSourceExist() {
    // Given
    var transformationFieldId = "instance.electronic.access.uri";
    var transformationPath = "$.source.uri";
    var defaultRuleMarcField = "856";
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(transformationFieldId);
    mappingTransformation.setPath(transformationPath);
    Rule defaultRule = new Rule();
    defaultRule.setField(defaultRuleMarcField);
    DataSource matchingDataSource = new DataSource();
    matchingDataSource.setFrom("$.instance.electronicAccess[*].uri");
    var expectedSubfield = "u";
    matchingDataSource.setSubfield(expectedSubfield);
    DataSource indicator1DataSource = new DataSource();
    indicator1DataSource.setIndicator("1");
    var indicator1Value = "4";
    Translation indicator1Translation = new Translation();
    indicator1Translation.setFunction("set_value");
    indicator1Translation.setParameters(Map.of("value", indicator1Value));
    indicator1DataSource.setTranslation(indicator1Translation);
    DataSource indicator2DataSource = new DataSource();
    indicator2DataSource.setIndicator("2");
    Translation indicator2Translation = new Translation();
    var indicator2Value = "0";
    indicator2Translation.setFunction("set_value");
    indicator2Translation.setParameters(Map.of("value", indicator2Value));
    indicator2DataSource.setTranslation(indicator2Translation);
    defaultRule.setDataSources(
        List.of(matchingDataSource, indicator1DataSource, indicator2DataSource));
    var defaultFieldId = "instance.electronic.access";
    CombinedRuleBuilder combinedRuleBuilder = spy(new CombinedRuleBuilder(3, defaultFieldId));
    doReturn(Optional.of(defaultRule))
        .when(combinedRuleBuilder)
        .build(any(List.class), eq(defaultFieldId));
    // When
    Optional<Rule> actualRuleOptional =
        combinedRuleBuilder.build(new ArrayList<>(), mappingTransformation);
    // Then
    assertTrue(actualRuleOptional.isPresent(), "A combined rule should be created.");
    Rule actualRule = actualRuleOptional.get();
    assertEquals(
        defaultRuleMarcField,
        actualRule.getField(),
        "The MARC field should be from the default rule.");
    assertEquals(
        3,
        actualRule.getDataSources().size(),
        "There should be 3 data sources (1 transformed, 2 indicators).");
    DataSource transformationDataSource =
        actualRule.getDataSources().stream()
            .filter(ds -> ds.getIndicator() == null)
            .findFirst()
            .orElseThrow();
    assertEquals(
        transformationPath,
        transformationDataSource.getFrom(),
        "The 'from' path should match the transformation path.");
    assertEquals(
        expectedSubfield,
        transformationDataSource.getSubfield(),
        "The subfield should be from the matched default data source.");
    assertTrue(
        actualRule.getDataSources().contains(indicator1DataSource),
        "Indicator 1 data source should be copied.");
    assertTrue(
        actualRule.getDataSources().contains(indicator2DataSource),
        "Indicator 2 data source should be copied.");
  }

  @Test
  @TestMate(name = "TestMate-9e4c1cd0143f5495c6277202f4e84d1a")
  void buildShouldReturnEmptyWhenDefaultRuleIsNotFound() {
    // Given
    var defaultFieldId = "instance.electronic.access";
    CombinedRuleBuilder combinedRuleBuilder = spy(new CombinedRuleBuilder(3, defaultFieldId));
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId("instance.electronic.access.uri");
    mappingTransformation.setPath("$.source.uri");
    doReturn(Optional.empty())
        .when(combinedRuleBuilder)
        .build(any(ArrayList.class), eq(defaultFieldId));
    // When
    Optional<Rule> actualResult =
        combinedRuleBuilder.build(new ArrayList<>(), mappingTransformation);
    // Then
    assertTrue(actualResult.isEmpty(), "The result should be an empty Optional.");
  }

  @Test
  @TestMate(name = "TestMate-26bd420f4939332db9bc6241c9557d13")
  void buildShouldReturnEmptyWhenSubfieldDataSourceIsNotFound() {
    // Given
    var transformationFieldId = "instance.electronic.access.nonexistent";
    var transformationPath = "$.source.nonexistent";
    var defaultRuleMarcField = "856";
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(transformationFieldId);
    mappingTransformation.setPath(transformationPath);
    Rule defaultRule = new Rule();
    defaultRule.setField(defaultRuleMarcField);
    DataSource nonMatchingDataSource = new DataSource();
    nonMatchingDataSource.setFrom("$.instance.electronicAccess[*].uri");
    nonMatchingDataSource.setSubfield("u");
    defaultRule.setDataSources(List.of(nonMatchingDataSource));
    var defaultFieldId = "instance.electronic.access";
    CombinedRuleBuilder combinedRuleBuilder = spy(new CombinedRuleBuilder(3, defaultFieldId));
    doReturn(Optional.of(defaultRule))
        .when(combinedRuleBuilder)
        .build(any(List.class), eq(defaultFieldId));
    // When
    Optional<Rule> actualResult =
        combinedRuleBuilder.build(new ArrayList<>(), mappingTransformation);
    // Then
    assertTrue(
        actualResult.isEmpty(),
        "The result should be an empty Optional as no matching subfield was found.");
  }
}
