package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.rest.resource.CleanUpFilesApi;
import org.folio.dataexp.service.StorageCleanUpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for cleaning up expired files and file definitions.
 */
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class CleanUpFilesController implements CleanUpFilesApi {
  private final StorageCleanUpService storageCleanUpService;

  /**
   * Cleans up expired files and file definitions.
   *
   * @return response entity with no content status
   */
  @Override
  public ResponseEntity<Void> postCleanUpFiles() {
    storageCleanUpService.cleanExpiredFilesAndFileDefinitions();
    return new ResponseEntity<>(
        HttpStatus.NO_CONTENT
    );
  }
}
