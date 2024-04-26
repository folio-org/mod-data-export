package org.folio.dataexp.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.folio.dataexp.domain.dto.Errors;
import org.folio.dataexp.exception.configuration.SliceSizeValidationException;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.exception.export.ExportDeletedDateRangeException;
import org.folio.dataexp.exception.file.definition.FileExtensionException;
import org.folio.dataexp.exception.file.definition.FileSizeException;
import org.folio.dataexp.exception.file.definition.UploadFileException;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
import org.folio.dataexp.exception.mapping.profile.DefaultMappingProfileException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileFieldsSuppressionException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileFieldsSuppressionPatternException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationEmptyException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationPatternException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

  @ExceptionHandler(DefaultMappingProfileException.class)
  public ResponseEntity<String> handleDefaultMappingProfileException(final DefaultMappingProfileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(MappingProfileTransformationPatternException.class)
  public ResponseEntity<Errors> handleMappingProfileValidationException(final MappingProfileTransformationPatternException e) {
    return new ResponseEntity<>(e.getErrors(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(MappingProfileTransformationEmptyException.class)
  public ResponseEntity<String> handleMappingProfileTransformationEmptyException(final MappingProfileTransformationEmptyException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(MappingProfileFieldsSuppressionPatternException.class)
  public ResponseEntity<Errors> handleMappingProfileFieldsSuppressionPatternException(final MappingProfileFieldsSuppressionPatternException e) {
    return new ResponseEntity<>(e.getErrors(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(MappingProfileFieldsSuppressionException.class)
  public ResponseEntity<Errors> handleMappingProfileFieldsSuppressionException(final MappingProfileFieldsSuppressionException e) {
    return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(DefaultJobProfileException.class)
  public ResponseEntity<String> handleDefaultJobProfileException(final DefaultJobProfileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleEntityNotFoundException(final EntityNotFoundException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(DataExportException.class)
  public ResponseEntity<String> handleDataExportException(final DataExportException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(TransformationValidationException.class)
  public ResponseEntity<String> handleTransformationValidationException(final TransformationValidationException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(SliceSizeValidationException.class)
  public ResponseEntity<String> handleConfigurationValidationException(final SliceSizeValidationException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> handleInvalidDateException() {
    return new ResponseEntity<>("Invalid date format for payload", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ExportDeletedDateRangeException.class)
  public ResponseEntity<String> handleInvalidDateRangeException(final ExportDeletedDateRangeException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
  }
}
