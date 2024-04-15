package org.folio.dataexp.exception.mapping.profile;

import lombok.Getter;
import org.folio.dataexp.domain.dto.Errors;

public class MappingProfileSuppressionFieldPatternException extends RuntimeException {
  @Getter
  private final Errors errors;

  public MappingProfileSuppressionFieldPatternException(String message, Errors errors) {
    super(message);
    this.errors = errors;
  }
}
