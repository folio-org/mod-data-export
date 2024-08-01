package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedRequest;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedResponse;
import org.folio.dataexp.rest.resource.ExportAuthorityDeletedApi;
import org.folio.dataexp.service.ExportAuthorityDeletedService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class DataExportAuthorityDeletedController implements ExportAuthorityDeletedApi {

  private final ExportAuthorityDeletedService exportAuthorityDeletedService;

  @Override
  public ResponseEntity<ExportAuthorityDeletedResponse> postExportDeletedAuthority(ExportAuthorityDeletedRequest request) {
    var response = exportAuthorityDeletedService.postExportDeletedAuthority(request);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
