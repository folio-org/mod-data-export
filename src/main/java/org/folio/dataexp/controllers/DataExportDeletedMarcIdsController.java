package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportDeletedMarcIdsRequest;
import org.folio.dataexp.rest.resource.ExportDeletedApi;
import org.folio.dataexp.service.ExportDeletedMarcIdsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class DataExportDeletedMarcIdsController implements ExportDeletedApi {

  private final ExportDeletedMarcIdsService exportDeletedMarcIdsService;

  @Override
  public ResponseEntity<Void> postExportDeletedMarcIds(ExportDeletedMarcIdsRequest request) {
    exportDeletedMarcIdsService.postExportDeletedMarcIds(request);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
