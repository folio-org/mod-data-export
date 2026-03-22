package org.folio.dataexp.service;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class CommonExportStatisticTest {

    @ParameterizedTest
  @CsvSource({
      "0, 5, 5",
      "5, 10, 15",
      "10, 0, 10"
  })
  void incrementDuplicatedUuidWithCountShouldIncreaseAmountByGivenValue(int initialAmount, int inputCount, int expectedFinalAmount) {
    // TestMate-087664f415be35b9612c6652f53b9515
    // Given
    var commonExportStatistic = new CommonExportStatistic();
    if (initialAmount > 0) {
      commonExportStatistic.incrementDuplicatedUuid(initialAmount);
    }
    // When
    commonExportStatistic.incrementDuplicatedUuid(inputCount);
    // Then
    assertThat(commonExportStatistic.getDuplicatedUuidAmount()).isEqualTo(expectedFinalAmount);
  }

}
