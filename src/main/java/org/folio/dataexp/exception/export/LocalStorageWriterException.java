package org.folio.dataexp.exception.export;

/**
 * Exception thrown when writing to local storage fails.
 */
public class LocalStorageWriterException extends RuntimeException {
  /**
   * Constructs a new LocalStorageWriterException with the specified detail message.
   *
   * @param message the detail message
   */
  public LocalStorageWriterException(String message) {
    super(message);
  }
}
