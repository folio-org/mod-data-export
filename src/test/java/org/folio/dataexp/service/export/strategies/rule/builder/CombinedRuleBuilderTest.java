package org.folio.dataexp.service.export.strategies.rule.builder;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.folio.processor.translations.Translation;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class CombinedRuleBuilderTest {

    @Test
  void buildShouldReturnCombinedRuleWhenDefaultRuleAndSubfieldDataSourceExist() {
    // TestMate-147dd2ffd2da942497c4e3b416a53011
    // Given
    final String TRANSFORMATION_FIELD_ID = "instance.electronic.access.uri";
    final String TRANSFORMATION_PATH = "$.source.uri";
    final String DEFAULT_FIELD_ID = "instance.electronic.access";
    final String DEFAULT_RULE_MARC_FIELD = "856";
    final String EXPECTED_SUBFIELD = "u";
    final String INDICATOR_1_VALUE = "4";
    final String INDICATOR_2_VALUE = "0";
    CombinedRuleBuilder combinedRuleBuilder = spy(new CombinedRuleBuilder(3, DEFAULT_FIELD_ID));
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(TRANSFORMATION_FIELD_ID);
    mappingTransformation.setPath(TRANSFORMATION_PATH);
    Rule defaultRule = new Rule();
    defaultRule.setField(DEFAULT_RULE_MARC_FIELD);
    DataSource matchingDataSource = new DataSource();
    matchingDataSource.setFrom("$.instance.electronicAccess[*].uri");
    matchingDataSource.setSubfield(EXPECTED_SUBFIELD);
    DataSource indicator1DataSource = new DataSource();
    indicator1DataSource.setIndicator("1");
    Translation indicator1Translation = new Translation();
    indicator1Translation.setFunction("set_value");
    indicator1Translation.setParameters(Map.of("value", INDICATOR_1_VALUE));
    indicator1DataSource.setTranslation(indicator1Translation);
    DataSource indicator2DataSource = new DataSource();
    indicator2DataSource.setIndicator("2");
    Translation indicator2Translation = new Translation();
    indicator2Translation.setFunction("set_value");
    indicator2Translation.setParameters(Map.of("value", INDICATOR_2_VALUE));
    indicator2DataSource.setTranslation(indicator2Translation);
    defaultRule.setDataSources(
        List.of(matchingDataSource, indicator1DataSource, indicator2DataSource));
    doReturn(Optional.of(defaultRule))
        .when(combinedRuleBuilder)
        .build(any(List.class), eq(DEFAULT_FIELD_ID));
    // When
    Optional<Rule> actualRuleOptional =
        combinedRuleBuilder.build(new ArrayList<>(), mappingTransformation);
    // Then
    assertTrue(actualRuleOptional.isPresent(), "A combined rule should be created.");
    Rule actualRule = actualRuleOptional.get();
    assertEquals(
        DEFAULT_RULE_MARC_FIELD,
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
        TRANSFORMATION_PATH,
        transformationDataSource.getFrom(),
        "The 'from' path should match the transformation path.");
    assertEquals(
        EXPECTED_SUBFIELD,
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
  void buildShouldReturnEmptyWhenDefaultRuleIsNotFound() {
    // TestMate-9e4c1cd0143f5495c6277202f4e84d1a
    // Given
    final String defaultFieldId = "instance.electronic.access";
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
  void buildShouldReturnEmptyWhenSubfieldDataSourceIsNotFound() {
    // TestMate-26bd420f4939332db9bc6241c9557d13
    // Given
    final String TRANSFORMATION_FIELD_ID = "instance.electronic.access.nonexistent";
    final String TRANSFORMATION_PATH = "$.source.nonexistent";
    final String DEFAULT_FIELD_ID = "instance.electronic.access";
    final String DEFAULT_RULE_MARC_FIELD = "856";
    CombinedRuleBuilder combinedRuleBuilder = spy(new CombinedRuleBuilder(3, DEFAULT_FIELD_ID));
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(TRANSFORMATION_FIELD_ID);
    mappingTransformation.setPath(TRANSFORMATION_PATH);
    Rule defaultRule = new Rule();
    defaultRule.setField(DEFAULT_RULE_MARC_FIELD);
    DataSource nonMatchingDataSource = new DataSource();
    nonMatchingDataSource.setFrom("$.instance.electronicAccess[*].uri");
    nonMatchingDataSource.setSubfield("u");
    defaultRule.setDataSources(List.of(nonMatchingDataSource));
    doReturn(Optional.of(defaultRule))
        .when(combinedRuleBuilder)
        .build(any(List.class), eq(DEFAULT_FIELD_ID));
    // When
    Optional<Rule> actualResult =
        combinedRuleBuilder.build(new ArrayList<>(), mappingTransformation);
    // Then
    assertTrue(
        actualResult.isEmpty(),
        "The result should be an empty Optional as no matching subfield was found.");
  }
}
