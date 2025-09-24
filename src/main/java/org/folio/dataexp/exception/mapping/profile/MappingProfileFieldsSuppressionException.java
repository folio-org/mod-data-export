package org.folio.dataexp.exception.mapping.profile;

/**
 * Exception thrown when mapping profile fields suppression fails.
 */
public class MappingProfileFieldsSuppressionException extends RuntimeException {
  /**
   * Constructs a new MappingProfileFieldsSuppressionException with the specified detail message.
   *
   * @param message the detail message
   */
  public MappingProfileFieldsSuppressionException(String message) {
    super(message);
  }
}
