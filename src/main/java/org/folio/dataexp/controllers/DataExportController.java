package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.rest.resource.ExportApi;
import org.folio.dataexp.service.DataExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class DataExportController implements  ExportApi {

  private final DataExportService dataExportService;

  @Override
  public ResponseEntity<Void> postDataExport(ExportRequest exportRequest) {
    dataExportService.postDataExport(exportRequest);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
