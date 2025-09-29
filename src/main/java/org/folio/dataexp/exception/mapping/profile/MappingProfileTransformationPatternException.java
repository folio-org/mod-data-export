package org.folio.dataexp.exception.mapping.profile;

import lombok.Getter;
import org.folio.dataexp.domain.dto.Errors;

/**
 * Exception thrown when mapping profile transformation pattern validation fails.
 */
public class MappingProfileTransformationPatternException extends RuntimeException {
  /**
   * Errors details for the transformation pattern exception.
   */
  @Getter
  private final Errors errors;

  /**
   * Constructs a new MappingProfileTransformationPatternException with the specified detail
   * message and errors.
   *
   * @param message the detail message
   * @param errors the errors details
   */
  public MappingProfileTransformationPatternException(String message, Errors errors) {
    super(message);
    this.errors = errors;
  }
}
