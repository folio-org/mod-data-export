package org.folio.dataexp.exception;

import jakarta.persistence.EntityNotFoundException;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class JobProfileExceptionHandler {

  @ExceptionHandler(DefaultJobProfileException.class)
  public ResponseEntity<String> handleDefaultJobProfileException(final DefaultJobProfileException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleEntityNotFoundException(final EntityNotFoundException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
  }
}
