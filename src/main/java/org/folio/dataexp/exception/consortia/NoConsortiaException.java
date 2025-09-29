package org.folio.dataexp.exception.consortia;

/**
 * Exception thrown when no consortia is found for the operation.
 */
public class NoConsortiaException extends RuntimeException {
  /**
   * Constructs a new NoConsortiaException with the specified detail message.
   *
   * @param msg the detail message
   */
  public NoConsortiaException(String msg) {
    super(msg);
  }
}
