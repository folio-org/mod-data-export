package org.folio.dataexp.exception.mapping.profile;

/** Exception thrown when mapping profile transformation is empty. */
public class MappingProfileTransformationEmptyException extends RuntimeException {
  /**
   * Constructs a new MappingProfileTransformationEmptyException with the specified detail message.
   *
   * @param message the detail message
   */
  public MappingProfileTransformationEmptyException(String message) {
    super(message);
  }
}
