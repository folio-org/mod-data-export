package org.folio.dataexp.exception;

import jakarta.persistence.EntityNotFoundException;
import org.folio.dataexp.domain.dto.Errors;
import org.folio.dataexp.exception.authority.AuthorityQueryException;
import org.folio.dataexp.exception.configuration.SliceSizeValidationException;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.exception.export.DownloadRecordException;
import org.folio.dataexp.exception.export.ExportDeletedDateRangeException;
import org.folio.dataexp.exception.file.definition.FileExtensionException;
import org.folio.dataexp.exception.file.definition.FileSizeException;
import org.folio.dataexp.exception.file.definition.UploadFileException;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
import org.folio.dataexp.exception.job.profile.LockedJobProfileException;
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

/** Global exception handler for data export related exceptions. */
@ControllerAdvice
public class DataExportExceptionHandler {

  /**
   * Handles file size exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(FileSizeException.class)
  public ResponseEntity<String> handleFileDefinitionSizeException(final FileSizeException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE);
  }

  /**
   * Handles file extension exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(FileExtensionException.class)
  public ResponseEntity<String> handleFileExtensionException(final FileExtensionException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Handles upload file exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(UploadFileException.class)
  public ResponseEntity<String> handleUploadFileException(final UploadFileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handles default mapping profile exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(DefaultMappingProfileException.class)
  public ResponseEntity<String> handleDefaultMappingProfileException(
      final DefaultMappingProfileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
  }

  /**
   * Handles mapping profile transformation pattern exceptions.
   *
   * @param e the exception
   * @return response entity with errors and status
   */
  @ExceptionHandler(MappingProfileTransformationPatternException.class)
  public ResponseEntity<Errors> handleMappingProfileValidationException(
      final MappingProfileTransformationPatternException e) {
    return new ResponseEntity<>(e.getErrors(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Handles mapping profile transformation empty exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(MappingProfileTransformationEmptyException.class)
  public ResponseEntity<String> handleMappingProfileTransformationEmptyException(
      final MappingProfileTransformationEmptyException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Handles mapping profile fields suppression pattern exceptions.
   *
   * @param e the exception
   * @return response entity with errors and status
   */
  @ExceptionHandler(MappingProfileFieldsSuppressionPatternException.class)
  public ResponseEntity<Errors> handleMappingProfileFieldsSuppressionPatternException(
      final MappingProfileFieldsSuppressionPatternException e) {
    return new ResponseEntity<>(e.getErrors(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Handles mapping profile fields suppression exceptions.
   *
   * @param e the exception
   * @return response entity with status
   */
  @ExceptionHandler(MappingProfileFieldsSuppressionException.class)
  public ResponseEntity<Errors> handleMappingProfileFieldsSuppressionException(
      final MappingProfileFieldsSuppressionException e) {
    return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Handles default job profile exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(DefaultJobProfileException.class)
  public ResponseEntity<String> handleDefaultJobProfileException(
      final DefaultJobProfileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
  }

  /**
   * Handles entity not found exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleEntityNotFoundException(final EntityNotFoundException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
  }

  /**
   * Handles data export exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(DataExportException.class)
  public ResponseEntity<String> handleDataExportException(final DataExportException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handles transformation validation exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(TransformationValidationException.class)
  public ResponseEntity<String> handleTransformationValidationException(
      final TransformationValidationException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Handles slice size validation exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(SliceSizeValidationException.class)
  public ResponseEntity<String> handleConfigurationValidationException(
      final SliceSizeValidationException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles invalid date format exceptions.
   *
   * @return response entity with error message and status
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> handleInvalidDateException() {
    return new ResponseEntity<>("Invalid date format for payload", HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles export deleted date range exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(ExportDeletedDateRangeException.class)
  public ResponseEntity<String> handleInvalidDateRangeException(
      final ExportDeletedDateRangeException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles authority query exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(AuthorityQueryException.class)
  public ResponseEntity<String> handleAuthorityQueryException(final AuthorityQueryException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles download record exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(DownloadRecordException.class)
  public ResponseEntity<String> handleDownloadRecordException(final DownloadRecordException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handles locked job profile exceptions.
   *
   * @param e the exception
   * @return response entity with error message and status
   */
  @ExceptionHandler(LockedJobProfileException.class)
  public ResponseEntity<String> handleLockedJobProfileException(final LockedJobProfileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
