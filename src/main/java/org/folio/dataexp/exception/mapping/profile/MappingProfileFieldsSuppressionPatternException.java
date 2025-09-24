package org.folio.dataexp.exception.mapping.profile;

import lombok.Getter;
import org.folio.dataexp.domain.dto.Errors;

/**
 * Exception thrown when mapping profile fields suppression pattern validation fails.
 */
public class MappingProfileFieldsSuppressionPatternException extends RuntimeException {
  /**
   * Errors details for the suppression pattern exception.
   */
  @Getter
  private final Errors errors;

  /**
   * Constructs a new MappingProfileFieldsSuppressionPatternException with the specified detail
   * message and errors.
   *
   * @param message the detail message
   * @param errors the errors details
   */
  public MappingProfileFieldsSuppressionPatternException(String message, Errors errors) {
    super(message);
    this.errors = errors;
  }
}
