package org.folio.dataexp.exception.export;

/** Exception thrown when an invalid deleted date range is provided for export. */
public class ExportDeletedDateRangeException extends RuntimeException {
  /**
   * Constructs a new ExportDeletedDateRangeException with the specified detail message.
   *
   * @param message the detail message
   */
  public ExportDeletedDateRangeException(String message) {
    super(message);
  }
}
