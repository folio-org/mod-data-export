package org.folio.dataexp.exception.file.definition;

/**
 * Exception thrown when a file upload operation fails.
 */
public class UploadFileException extends RuntimeException {
  /**
   * Constructs a new UploadFileException with the specified detail message.
   *
   * @param message the detail message
   */
  public UploadFileException(String message) {
    super(message);
  }
}
