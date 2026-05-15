package org.folio.dataexp.exception.file.definition;

/** Exception thrown when a file size is invalid. */
public class FileSizeException extends RuntimeException {
  /**
   * Constructs a new FileSizeException with the specified detail message.
   *
   * @param message the detail message
   */
  public FileSizeException(String message) {
    super(message);
  }
}
