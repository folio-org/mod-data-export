package org.folio.dataexp.exception;

import jakarta.persistence.EntityNotFoundException;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.exception.export.FileExtensionException;
import org.folio.dataexp.exception.export.FileSizeException;
import org.folio.dataexp.exception.export.UploadFileException;
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
  public ResponseEntity<String> handleUploadFileException(final UploadFileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleEntityNotFoundException(final EntityNotFoundException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(DataExportException.class)
  public ResponseEntity<String> handleDataExportException(final DataExportException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
