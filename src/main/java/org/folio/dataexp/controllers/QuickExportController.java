package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.QuickExportRequest;
import org.folio.dataexp.domain.dto.QuickExportResponse;
import org.folio.dataexp.rest.resource.QuickExportApi;
import org.folio.dataexp.service.QuickExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class QuickExportController implements QuickExportApi {

  private final QuickExportService quickExportService;

  @Override
  public ResponseEntity<QuickExportResponse> postDataExportQuickExport(QuickExportRequest quickExportRequest) {
    var response = quickExportService.postQuickExport(quickExportRequest);
    log.info("Quick export respone: {}", response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
