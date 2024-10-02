package org.folio.dataexp.controllers;


import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.IdType;
import org.folio.dataexp.rest.resource.DownloadRecordApi;
import org.folio.dataexp.service.DownloadRecordService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class DownloadRecordController implements DownloadRecordApi {

  private static final String UTF_FORMAT_POSTFIX = "-utf";
  private static final String MARC8_FORMAT_POSTFIX = "-marc8";

  private final DownloadRecordService downloadRecordService;


  @Override
  public ResponseEntity<Resource> downloadRecordById(UUID recordId, IdType idType, Boolean isUtf) {
    var formatPostfix = Boolean.TRUE.equals(isUtf) ? UTF_FORMAT_POSTFIX : MARC8_FORMAT_POSTFIX;
    var resource = downloadRecordService.processRecordDownload(recordId, isUtf, formatPostfix, idType);
    var fileName = recordId + formatPostfix + ".mrc";
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(resource);
  }
}
