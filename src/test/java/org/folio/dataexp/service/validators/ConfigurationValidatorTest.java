package org.folio.dataexp.service.validators;

import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.exception.configuration.SliceSizeValidationException;
import org.folio.dataexp.service.SlicerProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurationValidatorTest {

  @Test
  void validateSliceSizeLessThanOneTest() {
    var configurationValidator = new ConfigurationValidator();
    var config = new Config().key(SlicerProcessor.SLICE_SIZE_KEY).value("0");
    assertThrows(SliceSizeValidationException.class, () -> configurationValidator.validate(config));
  }

  @Test
  void validateSliceSizeNotANumberTest() {
    var configurationValidator = new ConfigurationValidator();
    var config = new Config().key(SlicerProcessor.SLICE_SIZE_KEY).value("a50000");
    assertThrows(SliceSizeValidationException.class, () -> configurationValidator.validate(config));
  }
}
