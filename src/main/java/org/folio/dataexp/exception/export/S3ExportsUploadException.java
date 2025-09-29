package org.folio.dataexp.exception.export;

/**
 * Exception thrown when uploading exports to S3 fails.
 */
public class S3ExportsUploadException extends RuntimeException {
  /**
   * Constructs a new S3ExportsUploadException with the specified detail message.
   *
   * @param message the detail message
   */
  public S3ExportsUploadException(String message) {
    super(message);
  }
}
