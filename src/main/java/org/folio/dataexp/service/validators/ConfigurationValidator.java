package org.folio.dataexp.service.validators;

import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.exception.configuration.SliceSizeValidationException;
import org.folio.dataexp.service.SlicerProcessor;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ConfigurationValidator {

  public void validate(Config config) {
    if (config.getKey().equals(SlicerProcessor.SLICE_SIZE_KEY)) {
      try {
        var value = Integer.parseInt(config.getValue());
        if (value < 1) {
          var errorMsg = String.format("Slice size value cannot be less than 1: %d", value);
          log.error(errorMsg);
          throw new SliceSizeValidationException(errorMsg);
        }
      } catch (NumberFormatException e) {
        var errorMsg = String.format("Slice size is not a number: %s", config.getValue());
        log.error(errorMsg);
        throw new SliceSizeValidationException(errorMsg);
      }
    }
  }
}
