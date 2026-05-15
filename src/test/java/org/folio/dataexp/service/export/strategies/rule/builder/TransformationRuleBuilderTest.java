package org.folio.dataexp.service.export.strategies.rule.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.ErrorCode.ERROR_RULE_NO_INDICATORS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TransformationRuleBuilderTest {

  private static Stream<Arguments> itemTypeRuleFlagScenarios() {
    // Scenario 2 setup
    var existingRule = new Rule();
    existingRule.setField("900");
    existingRule.setIndicators("ff");
    existingRule.setItemTypeRule(false);
    existingRule.setDataSources(new ArrayList<>());
    var rulesWithExisting = new ArrayList<Rule>();
    rulesWithExisting.add(existingRule);
    return Stream.of(
        // Scenario 1: New ITEM Rule
        Arguments.of(new ArrayList<Rule>(), RecordTypes.ITEM, true),
        // Scenario 2: Update Existing Rule to ITEM Rule
        Arguments.of(rulesWithExisting, RecordTypes.ITEM, true),
        // Scenario 3: New Non-ITEM Rule
        Arguments.of(new ArrayList<Rule>(), RecordTypes.INSTANCE, false));
  }

  private static Stream<Arguments> translationFunctionScenarios() {
    return Stream.of(
        Arguments.of("instance.materialtypeid", "set_material_type"),
        Arguments.of("holdings.permanentlocation.name", "set_location"));
  }

  @Test
  @TestMate(name = "TestMate-50149264138c9b470c2cf9a7a31088c3")
  void buildShouldCreateNewRuleWhenNoExistingRuleMatches() throws TransformationRuleException {
    // Given
    var transformationRuleBuilder = new TransformationRuleBuilder();
    var rules = new ArrayList<Rule>();
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("900ff$a");
    mappingTransformation.setPath("$.instance.id");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "The resulting Optional<Rule> should not be empty");
    Rule createdRule = resultRuleOptional.get();
    assertEquals("900", createdRule.getField());
    assertEquals("ff", createdRule.getIndicators());
    assertFalse(
        createdRule.isItemTypeRule(), "isItemTypeRule should be false for INSTANCE record type");
    List<DataSource> dataSources = createdRule.getDataSources();
    assertThat(dataSources).hasSize(3);
    // Verify the data source for the subfield '$a'
    DataSource subfieldDataSource = dataSources.get(0);
    assertEquals("a", subfieldDataSource.getSubfield());
    assertEquals("$.instance.id", subfieldDataSource.getFrom());
    assertNull(subfieldDataSource.getIndicator());
    // Verify the data source for indicator '1'
    Optional<DataSource> indicator1DataSourceOpt =
        dataSources.stream().filter(ds -> "1".equals(ds.getIndicator())).findFirst();
    assertTrue(indicator1DataSourceOpt.isPresent(), "Data source for indicator '1' should exist");
    DataSource indicator1DataSource = indicator1DataSourceOpt.get();
    assertEquals("set_value", indicator1DataSource.getTranslation().getFunction());
    assertThat(indicator1DataSource.getTranslation().getParameters()).containsEntry("value", "f");
    // Verify the data source for indicator '2'
    Optional<DataSource> indicator2DataSourceOpt =
        dataSources.stream().filter(ds -> "2".equals(ds.getIndicator())).findFirst();
    assertTrue(indicator2DataSourceOpt.isPresent(), "Data source for indicator '2' should exist");
    DataSource indicator2DataSource = indicator2DataSourceOpt.get();
    assertEquals("set_value", indicator2DataSource.getTranslation().getFunction());
    assertThat(indicator2DataSource.getTranslation().getParameters()).containsEntry("value", "f");
  }

  @Test
  @TestMate(name = "TestMate-330ae08fcde919fd7e0df336a4315042")
  void buildShouldAppendToExistingRuleWhenFieldAndIndicatorsMatch()
      throws TransformationRuleException {
    // Given
    var initialDataSource = new DataSource();
    initialDataSource.setFrom("$.instance.id");
    initialDataSource.setSubfield("a");
    var existingRule = new Rule();
    existingRule.setField("900");
    existingRule.setIndicators("ff");
    var dataSources = new ArrayList<DataSource>();
    dataSources.add(initialDataSource);
    existingRule.setDataSources(dataSources);
    var rules = new ArrayList<Rule>();
    rules.add(existingRule);
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("900ff$b");
    mappingTransformation.setPath("$.instance.hrid");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    var transformationRuleBuilder = new TransformationRuleBuilder();
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "The resulting Optional<Rule> should not be empty");
    Rule returnedRule = resultRuleOptional.get();
    assertThat(returnedRule).isSameAs(existingRule);
    assertThat(returnedRule.getDataSources()).hasSize(2);
    DataSource newDataSource =
        returnedRule.getDataSources().stream()
            .filter(ds -> "b".equals(ds.getSubfield()))
            .findFirst()
            .orElseThrow();
    assertEquals("b", newDataSource.getSubfield());
    assertEquals("$.instance.hrid", newDataSource.getFrom());
  }

  @Test
  @TestMate(name = "TestMate-97865dbaab229b626052d786076545c7")
  void buildShouldCreateNewRuleWhenFieldMatchesButIndicatorsDiffer()
      throws TransformationRuleException {
    // Given
    var existingDataSource = new DataSource();
    existingDataSource.setFrom("$.existing.path");
    existingDataSource.setSubfield("z");
    var existingRule = new Rule();
    existingRule.setField("900");
    existingRule.setIndicators("ff");
    existingRule.setDataSources(new ArrayList<>(List.of(existingDataSource)));
    var rules = new ArrayList<Rule>();
    rules.add(existingRule);
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("90011$a");
    mappingTransformation.setPath("$.instance.id");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    var transformationRuleBuilder = new TransformationRuleBuilder();
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "A rule should be created");
    Rule createdRule = resultRuleOptional.get();
    assertThat(createdRule).isNotSameAs(existingRule);
    assertEquals("900", createdRule.getField());
    assertEquals("11", createdRule.getIndicators());
    assertFalse(createdRule.isItemTypeRule());
    List<DataSource> createdDataSources = createdRule.getDataSources();
    assertThat(createdDataSources).hasSize(3);
    DataSource subfieldDataSource =
        createdDataSources.stream()
            .filter(ds -> "a".equals(ds.getSubfield()))
            .findFirst()
            .orElseThrow();
    assertEquals("$.instance.id", subfieldDataSource.getFrom());
    DataSource indicator1DataSource =
        createdDataSources.stream()
            .filter(ds -> "1".equals(ds.getIndicator()))
            .findFirst()
            .orElseThrow();
    assertEquals("set_value", indicator1DataSource.getTranslation().getFunction());
    assertThat(indicator1DataSource.getTranslation().getParameters()).containsEntry("value", "1");
    DataSource indicator2DataSource =
        createdDataSources.stream()
            .filter(ds -> "2".equals(ds.getIndicator()))
            .findFirst()
            .orElseThrow();
    assertEquals("set_value", indicator2DataSource.getTranslation().getFunction());
    assertThat(indicator2DataSource.getTranslation().getParameters()).containsEntry("value", "1");
    assertThat(existingRule.getDataSources()).hasSize(1);
  }

  @Test
  @TestMate(name = "TestMate-9d539f2dd9b2f6652f471ccd75ddca08")
  void buildShouldThrowExceptionWhenExistingRuleHasNullIndicators() {
    // Given
    var invalidRule = new Rule();
    invalidRule.setField("900");
    invalidRule.setIndicators(null);
    var rules = new ArrayList<Rule>();
    rules.add(invalidRule);
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("900ff$a");
    var transformationRuleBuilder = new TransformationRuleBuilder();
    // When & Then
    var exception =
        assertThrows(
            TransformationRuleException.class,
            () -> transformationRuleBuilder.build(rules, mappingTransformation));
    String expectedMessage = ERROR_RULE_NO_INDICATORS.getDescription().formatted("900");
    assertEquals(expectedMessage, exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("itemTypeRuleFlagScenarios")
  @TestMate(name = "TestMate-2b6c9986afec2112e03439591196293b")
  void buildShouldSetItemTypeRuleFlagCorrectly(
      ArrayList<Rule> initialRules, RecordTypes recordType, boolean expectedIsItemTypeRule)
      throws TransformationRuleException {
    // Given
    var transformationRuleBuilder = new TransformationRuleBuilder();
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("900ff$a");
    mappingTransformation.setPath("$.item.id");
    mappingTransformation.setRecordType(recordType);
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(initialRules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "A rule should be created or updated");
    Rule resultRule = resultRuleOptional.get();
    assertEquals(
        expectedIsItemTypeRule,
        resultRule.isItemTypeRule(),
        "The isItemTypeRule flag should be " + expectedIsItemTypeRule);
    if (recordType == RecordTypes.ITEM && !initialRules.isEmpty()) {
      assertThat(resultRule).isSameAs(initialRules.get(0));
    }
  }

  @Test
  @TestMate(name = "TestMate-ea4472693a769cc88bbc2838983e3255")
  void buildShouldAddMetadataToRuleWhenPresentInTransformation()
      throws TransformationRuleException {
    // Given
    var metadata = new HashMap<String, String>();
    metadata.put("createdDate", "$.instance.metadata.createdDate");
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("901ff$a");
    mappingTransformation.setPath("$.instance.id");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    mappingTransformation.setMetadataParameters(metadata);
    var rules = new ArrayList<Rule>();
    var transformationRuleBuilder = new TransformationRuleBuilder();
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "A rule should have been created");
    Rule createdRule = resultRuleOptional.get();
    assertThat(createdRule.getMetadata()).isNotNull();
    assertThat(createdRule.getMetadata().getData()).hasSize(1).containsKey("createdDate");
    assertThat(createdRule.getMetadata().getData().get("createdDate").getFrom())
        .isEqualTo("$.instance.metadata.createdDate");
  }

  @ParameterizedTest
  @MethodSource("translationFunctionScenarios")
  @TestMate(name = "TestMate-0960c0f13f4728cc2d28cba6d1308a74")
  void buildShouldApplyTranslationForMatchingFieldId(String fieldId, String expectedFunction)
      throws TransformationRuleException {
    // Given
    var transformationRuleBuilder = new TransformationRuleBuilder();
    var rules = new ArrayList<Rule>();
    var mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(fieldId);
    mappingTransformation.setTransformation("900ff$a");
    mappingTransformation.setPath("$.some.path");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "A rule should have been created");
    Rule createdRule = resultRuleOptional.get();
    DataSource dataSource =
        createdRule.getDataSources().stream()
            .filter(ds -> "a".equals(ds.getSubfield()))
            .findFirst()
            .orElseThrow();
    assertThat(dataSource.getTranslation()).isNotNull();
    assertEquals(expectedFunction, dataSource.getTranslation().getFunction());
  }

  @Test
  @TestMate(name = "TestMate-574da94c950fbac97f1e0bc53a9368b0")
  void buildShouldNotApplyAnyTranslationWhenFieldIdDoesNotMatch()
      throws TransformationRuleException {
    // Given
    var transformationRuleBuilder = new TransformationRuleBuilder();
    var rules = new ArrayList<Rule>();
    var mappingTransformation = new Transformations();
    mappingTransformation.setFieldId("instance.title");
    mappingTransformation.setTransformation("245  $a");
    mappingTransformation.setPath("$.instance.title");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "A rule should have been created");
    Rule createdRule = resultRuleOptional.get();
    DataSource dataSource =
        createdRule.getDataSources().stream()
            .filter(ds -> "a".equals(ds.getSubfield()))
            .findFirst()
            .orElseThrow();
    assertNull(
        dataSource.getTranslation(), "Translation should be null for a non-matching fieldId");
  }

  @Test
  @TestMate(name = "TestMate-7735df8988f9dc177a71ebb2b566f2f0")
  void buildShouldHandleTransformationWithoutSubfield() throws TransformationRuleException {
    // Given
    var transformationRuleBuilder = new TransformationRuleBuilder();
    var rules = new ArrayList<Rule>();
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("500  ");
    mappingTransformation.setPath("$.instance.notes.note");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "A rule should have been created");
    Rule createdRule = resultRuleOptional.get();
    assertEquals("500", createdRule.getField());
    assertEquals("  ", createdRule.getIndicators());
    List<DataSource> dataSources = createdRule.getDataSources();
    assertThat(dataSources).hasSize(1);
    DataSource primaryDataSource = dataSources.get(0);
    assertEquals("$.instance.notes.note", primaryDataSource.getFrom());
    assertNull(primaryDataSource.getSubfield());
    // In the absence of a subfield, indicator data sources are not created by the current
    // implementation.
    // Therefore, we only verify the primary data source.
  }

  @Test
  @TestMate(name = "TestMate-4a5f32490fe0e02ceaca01654dc80f3b")
  void buildShouldSortDataSourcesBySubfield() throws TransformationRuleException {
    // Given
    var dataSourceB = new DataSource();
    dataSourceB.setSubfield("b");
    dataSourceB.setFrom("path.for.b");
    var dataSource1 = new DataSource();
    dataSource1.setSubfield("1");
    dataSource1.setFrom("path.for.1");
    var unsortedDataSources = new ArrayList<DataSource>();
    unsortedDataSources.add(dataSourceB);
    unsortedDataSources.add(dataSource1);
    var existingRule = new Rule();
    existingRule.setField("900");
    existingRule.setIndicators("ff");
    existingRule.setDataSources(unsortedDataSources);
    var rules = new ArrayList<Rule>();
    rules.add(existingRule);
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("900ff$a");
    mappingTransformation.setPath("path.for.a");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    var transformationRuleBuilder = new TransformationRuleBuilder();
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "The resulting Optional<Rule> should not be empty");
    Rule updatedRule = resultRuleOptional.get();
    assertThat(updatedRule).isSameAs(existingRule);
    List<DataSource> dataSources = updatedRule.getDataSources();
    assertThat(dataSources).hasSize(3);
    List<String> subfields = dataSources.stream().map(DataSource::getSubfield).toList();
    assertThat(subfields).containsExactly("a", "b", "1");
  }

  @Test
  @TestMate(name = "TestMate-8f3c0b8ff8d064c6f86cb2543cf0f24f")
  void buildShouldNotUpdateItemTypeRuleFlagIfAlreadyTrue() throws TransformationRuleException {
    // Given
    var existingDataSource = new DataSource();
    existingDataSource.setSubfield("a");
    var existingRule = new Rule();
    existingRule.setField("900");
    existingRule.setIndicators("ff");
    existingRule.setItemTypeRule(true);
    var dataSources = new ArrayList<DataSource>();
    dataSources.add(existingDataSource);
    existingRule.setDataSources(dataSources);
    var rules = new ArrayList<Rule>();
    rules.add(existingRule);
    var mappingTransformation = new Transformations();
    mappingTransformation.setTransformation("900ff$b");
    mappingTransformation.setPath("$.instance.hrid");
    mappingTransformation.setRecordType(RecordTypes.INSTANCE);
    var transformationRuleBuilder = new TransformationRuleBuilder();
    // When
    Optional<Rule> resultRuleOptional =
        transformationRuleBuilder.build(rules, mappingTransformation);
    // Then
    assertTrue(resultRuleOptional.isPresent(), "The resulting Optional<Rule> should not be empty");
    Rule returnedRule = resultRuleOptional.get();
    assertThat(returnedRule).isSameAs(existingRule);
    assertThat(returnedRule.getDataSources()).hasSize(2);
    assertTrue(
        returnedRule.isItemTypeRule(),
        "The isItemTypeRule flag should remain true after an INSTANCE transformation");
  }
}
