package org.folio.dataexp.controllers;


import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.rest.resource.DownloadAuthorityApi;
import org.folio.dataexp.service.DownloadRecordService;
import org.springframework.core.io.ByteArrayResource;
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
public class DownloadAuthorityController implements DownloadAuthorityApi {

  private static final String UTF_FORMAT_POSTFIX = "-utf";
  private static final String MARC8_FORMAT_POSTFIX = "-marc8";

  private final DownloadRecordService downloadRecordService;

  @Override
  public ResponseEntity<Resource> downloadAuthorityById(UUID authorityId, Boolean isUtf) {
    var formatPostfix = Boolean.TRUE.equals(isUtf) ? UTF_FORMAT_POSTFIX : MARC8_FORMAT_POSTFIX;
    ByteArrayResource resource = downloadRecordService.processAuthorityDownload(authorityId, isUtf, formatPostfix);
    String fileName = authorityId + formatPostfix + ".mrc";
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(resource);
  }
}
