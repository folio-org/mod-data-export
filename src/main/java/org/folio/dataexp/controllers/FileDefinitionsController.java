package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.rest.resource.FileDefinitionsApi;
import org.folio.dataexp.service.FileDefinitionsService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class FileDefinitionsController implements FileDefinitionsApi {

  private final FileDefinitionsService fileDefinitionsService;

  @Override
  public ResponseEntity<FileDefinition> postFileDefinition(FileDefinition fileDefinition) {
    var savedFileDefinition = fileDefinitionsService.postFileDefinition(fileDefinition);
    return new ResponseEntity<>(savedFileDefinition, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<FileDefinition> getFileDefinitionById(UUID fileDefinitionId) {
    return new ResponseEntity<>(fileDefinitionsService.getFileDefinitionById(fileDefinitionId), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<FileDefinition> uploadFile(UUID fileDefinitionId, Resource resource) {
    var fileDefinition = fileDefinitionsService.uploadFile(fileDefinitionId, resource);
    return new ResponseEntity<>(fileDefinition, HttpStatus.OK);
  }
}
