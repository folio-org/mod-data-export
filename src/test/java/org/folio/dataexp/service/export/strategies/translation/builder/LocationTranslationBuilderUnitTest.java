package org.folio.dataexp.service.export.strategies.translation.builder;

import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.translations.Translation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.folio.dataexp.service.export.strategies.translation.builder.LocationTranslationBuilder;
import org.junit.jupiter.params.provider.ValueSource;

class LocationTranslationBuilderUnitTest {

    @Test
  void testBuildWhenFieldIdHasThreePartsShouldExtractFieldParameter() {
    // TestMate-7acf258d53bcc4ae0b5c50c4c06ed3a7
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
    assertThat(actualTranslation.getParameters())
        .hasSize(1)
        .containsEntry("field", "name");
  }

    @ParameterizedTest
  @CsvSource({
    "holdings.location.library.name, library, name, loclibs, libraryId",
    "item.location.campus.code, campus, code, loccamps, campusId",
    "holdings.location.institution.id, institution, id, locinsts, institutionId"
  })
  void testBuildWhenFieldIdHasFourPartsShouldSetReferenceDataParameters(String fieldId, String referenceDataType, String expectedField, String expectedRefData, String expectedRefDataIdField) {
    // TestMate-1603e70078908c7a69ecd3a9c50206f1
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
        .containsEntry("referenceDataIdField", expectedRefDataIdField);
  }

    @Test
  void testBuildWhenFieldIdHasFourPartsButUnknownTypeShouldOnlySetFieldParameter() {
    // TestMate-35b4dce467cb376bcfbc44c8eee9dc3b
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
  @ValueSource(strings = {
    "holdings",
    "holdings.location",
    "holdings.location.library.name.extra",
    "a.b.c.d.e"
  })
  void testBuildWhenFieldIdHasUnexpectedPartCountShouldReturnEmptyParameters(String fieldId) {
    // TestMate-ca2c3a5fe9a14337bc55ea0e6e4df0ed
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

}
