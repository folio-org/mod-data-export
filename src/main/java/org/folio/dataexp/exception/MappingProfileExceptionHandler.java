package org.folio.dataexp.exception;

import jakarta.persistence.EntityNotFoundException;
import org.folio.dataexp.exception.mapping.profile.DefaultMappingProfileException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MappingProfileExceptionHandler {
  @ExceptionHandler(DefaultMappingProfileException.class)
  public ResponseEntity<String> handleDefaultMappingProfileException(final DefaultMappingProfileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleEntityNotFoundException(final EntityNotFoundException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
  }
}
