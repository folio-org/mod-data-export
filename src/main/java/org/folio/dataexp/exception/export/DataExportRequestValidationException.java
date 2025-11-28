package org.folio.dataexp.exception.export;

/** Exception thrown when a data export request fails validation. */
public class DataExportRequestValidationException extends RuntimeException {
  /**
   * Constructs a new DataExportRequestValidationException with the specified detail message.
   *
   * @param message the detail message
   */
  public DataExportRequestValidationException(String message) {
    super(message);
  }
}
