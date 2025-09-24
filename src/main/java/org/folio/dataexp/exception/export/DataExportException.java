package org.folio.dataexp.exception.export;

/**
 * Exception thrown when a data export operation fails.
 */
public class DataExportException extends RuntimeException {
  /**
   * Constructs a new DataExportException with the specified detail message.
   *
   * @param message the detail message
   */
  public DataExportException(String message) {
    super(message);
  }
}
