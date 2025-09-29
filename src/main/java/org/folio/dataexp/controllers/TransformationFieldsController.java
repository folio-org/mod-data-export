package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.TransformationFieldCollection;
import org.folio.dataexp.rest.resource.TransformationFieldsApi;
import org.folio.dataexp.service.transformationfields.TransformationFieldsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for transformation fields operations.
 */
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class TransformationFieldsController implements TransformationFieldsApi {
  private final TransformationFieldsService transformationFieldsService;

  /**
   * Gets transformation fields.
   *
   * @return response entity with transformation field collection
   */
  @Override
  public ResponseEntity<TransformationFieldCollection> getTransformationFields() {
    return new ResponseEntity<>(
        transformationFieldsService.getTransformationFields(),
        HttpStatus.OK
    );
  }
}
