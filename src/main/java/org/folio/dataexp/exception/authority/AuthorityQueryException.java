package org.folio.dataexp.exception.authority;

/** Exception thrown when an error occurs during authority query operations. */
public class AuthorityQueryException extends RuntimeException {
  /**
   * Constructs a new AuthorityQueryException with the specified detail message.
   *
   * @param msg the detail message
   */
  public AuthorityQueryException(String msg) {
    super(msg);
  }
}
