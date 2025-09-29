package org.folio.dataexp.exception.export;

/**
 * Exception thrown when a record download operation fails.
 */
public class DownloadRecordException extends RuntimeException {
  /**
   * Constructs a new DownloadRecordException with the specified detail message.
   *
   * @param message the detail message
   */
  public DownloadRecordException(String message) {
    super(message);
  }
}
