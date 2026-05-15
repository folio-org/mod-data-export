package org.folio.dataexp.exception.file.definition;

/** Exception thrown when a file extension is invalid. */
public class FileExtensionException extends RuntimeException {
  /**
   * Constructs a new FileExtensionException with the specified detail message.
   *
   * @param message the detail message
   */
  public FileExtensionException(String message) {
    super(message);
  }
}
