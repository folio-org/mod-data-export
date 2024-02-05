package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.rest.resource.ExportAllApi;
import org.folio.dataexp.service.DataExportAllService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class DataExportAllController implements ExportAllApi {

  private final DataExportAllService dataExportAllService;

  @Override
  public ResponseEntity<Void> postExportAll(ExportAllRequest exportAllRequest) {
    dataExportAllService.postDataExportAll(exportAllRequest);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
