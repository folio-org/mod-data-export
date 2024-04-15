package org.folio.dataexp.exception;

import jakarta.persistence.EntityNotFoundException;
import org.folio.dataexp.domain.dto.Errors;
import org.folio.dataexp.exception.configuration.SliceSizeValidationException;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.exception.file.definition.FileExtensionException;
import org.folio.dataexp.exception.file.definition.FileSizeException;
import org.folio.dataexp.exception.file.definition.UploadFileException;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
import org.folio.dataexp.exception.mapping.profile.DefaultMappingProfileException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileSuppressionFieldPatternException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationEmptyException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationPatternException;
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

  @ExceptionHandler(MappingProfileSuppressionFieldPatternException.class)
  public ResponseEntity<Errors> handleMappingProfileSuppressionFieldPatternException(final MappingProfileSuppressionFieldPatternException e) {
    return new ResponseEntity<>(e.getErrors(), HttpStatus.UNPROCESSABLE_ENTITY);
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
}
