package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.rest.resource.ExportApi;
import org.folio.dataexp.rest.resource.FileDefinitionsApi;
import org.folio.dataexp.service.DataExportService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class DataExportController implements FileDefinitionsApi, ExportApi {

  private final DataExportService dataExportService;

  @Override
  public ResponseEntity<FileDefinition> postFileDefinition(FileDefinition fileDefinition) {
    var savedFileDefinition = dataExportService.postFileDefinition(fileDefinition);
    return new ResponseEntity<>(savedFileDefinition, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<FileDefinition> getFileDefinitionById(UUID fileDefinitionId) {
    return new ResponseEntity<>(dataExportService.getFileDefinitionById(fileDefinitionId), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<FileDefinition> uploadFile(UUID fileDefinitionId, Resource resource) {
    var fileDefinition = dataExportService.uploadFile(fileDefinitionId, resource);
    return new ResponseEntity<>(fileDefinition, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> postDataExport(ExportRequest exportRequest) {
    dataExportService.postDataExport(exportRequest);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public Optional<NativeWebRequest> getRequest() {
    return Optional.empty();
  }
}
