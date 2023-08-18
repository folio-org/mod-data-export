package org.folio.dataexp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class DataExportExceptionHandler {

  @ExceptionHandler(FileSizeException.class)
  public ResponseEntity<String> handleFileDefinitionSizeException(final FileSizeException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE);
  }

  @ExceptionHandler(FileExtensionException.class)
  public ResponseEntity<String> handleFileExtensionException(final FileExtensionException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(UploadFileException.class)
  public ResponseEntity<String> handleUploadFileExtensionException(final FileExtensionException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
  }
}
