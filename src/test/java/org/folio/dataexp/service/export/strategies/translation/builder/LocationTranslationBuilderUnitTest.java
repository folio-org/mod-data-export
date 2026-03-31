package org.folio.dataexp.service.export.strategies.translation.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.service.export.strategies.translation.builder.LocationTranslationBuilder.CAMPUSES;
import static org.folio.dataexp.service.export.strategies.translation.builder.LocationTranslationBuilder.INSTITUTIONS;
import static org.folio.dataexp.service.export.strategies.translation.builder.LocationTranslationBuilder.LIBRARIES;

import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.translations.Translation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LocationTranslationBuilderUnitTest {

  @Test
  @TestMate(name = "TestMate-7acf258d53bcc4ae0b5c50c4c06ed3a7")
  void testBuildWhenFieldIdHasThreePartsShouldExtractFieldParameter() {
    // Given
    LocationTranslationBuilder builder = new LocationTranslationBuilder();
    String functionName = "set_location";
    String fieldId = "holdings.location.name";
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(fieldId);
    // When
    Translation actualTranslation = builder.build(functionName, mappingTransformation);
    // Then
    assertThat(actualTranslation.getFunction()).isEqualTo(functionName);
    assertThat(actualTranslation.getParameters()).hasSize(1).containsEntry("field", "name");
  }

  @Test
  @TestMate(name = "TestMate-35b4dce467cb376bcfbc44c8eee9dc3b")
  void testBuildWhenFieldIdHasFourPartsButUnknownTypeShouldOnlySetFieldParameter() {
    // Given
    LocationTranslationBuilder builder = new LocationTranslationBuilder();
    String functionName = "set_location";
    String fieldId = "holdings.location.unknown.name";
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(fieldId);
    // When
    Translation actualTranslation = builder.build(functionName, mappingTransformation);
    // Then
    assertThat(actualTranslation.getFunction()).isEqualTo(functionName);
    assertThat(actualTranslation.getParameters())
        .hasSize(1)
        .containsEntry("field", "name")
        .doesNotContainKey("referenceData")
        .doesNotContainKey("referenceDataIdField");
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-ca2c3a5fe9a14337bc55ea0e6e4df0ed")
  @ValueSource(
      strings = {
        "holdings",
        "holdings.location",
        "holdings.location.library.name.extra",
        "a.b.c.d.e"
      })
  void testBuildWhenFieldIdHasUnexpectedPartCountShouldReturnEmptyParameters(String fieldId) {
    // Given
    LocationTranslationBuilder builder = new LocationTranslationBuilder();
    String functionName = "set_location";
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(fieldId);
    // When
    Translation actualTranslation = builder.build(functionName, mappingTransformation);
    // Then
    assertThat(actualTranslation.getFunction()).isEqualTo(functionName);
    assertThat(actualTranslation.getParameters()).isEmpty();
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-57583535e79e5e93392046c7acca9a3a")
  @CsvSource({
    "holdings.location.library.name, name, " + LIBRARIES + ", libraryId",
    "holdings.location.campus.code, code, " + CAMPUSES + ", campusId",
    "holdings.location.institution.name, name, " + INSTITUTIONS + ", institutionId"
  })
  void testBuildWhenFieldIdHasFourPartsShouldSetReferenceDataForKnownTypes(
      String fieldId, String expectedField, String expectedRefData, String expectedRefIdField) {
    // Given
    LocationTranslationBuilder builder = new LocationTranslationBuilder();
    String functionName = "set_location";
    Transformations mappingTransformation = new Transformations();
    mappingTransformation.setFieldId(fieldId);
    // When
    Translation actualTranslation = builder.build(functionName, mappingTransformation);
    // Then
    assertThat(actualTranslation.getFunction()).isEqualTo(functionName);
    assertThat(actualTranslation.getParameters())
        .hasSize(3)
        .containsEntry("field", expectedField)
        .containsEntry("referenceData", expectedRefData)
        .containsEntry("referenceDataIdField", expectedRefIdField);
  }
}
