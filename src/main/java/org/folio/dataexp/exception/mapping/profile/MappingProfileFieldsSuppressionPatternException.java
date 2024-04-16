package org.folio.dataexp.exception.mapping.profile;

import lombok.Getter;
import org.folio.dataexp.domain.dto.Errors;

public class MappingProfileFieldsSuppressionPatternException extends RuntimeException {
  @Getter
  private final Errors errors;

  public MappingProfileFieldsSuppressionPatternException(String message, Errors errors) {
    super(message);
    this.errors = errors;
  }
}
