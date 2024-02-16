package org.folio.dataexp.exception.export;

public class S3ExportsUploadException extends RuntimeException {

  public S3ExportsUploadException(String message) {
    super(message);
  }
}
