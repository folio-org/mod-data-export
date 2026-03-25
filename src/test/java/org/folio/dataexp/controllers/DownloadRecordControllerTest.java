package org.folio.dataexp.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.IdType;
import org.folio.dataexp.service.DownloadRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class DownloadRecordControllerTest {

  @Mock private DownloadRecordService downloadRecordService;

  @InjectMocks private DownloadRecordController downloadRecordController;

  @Test
  @TestMate(name = "TestMate-c3c26014fe1d31e72f0a453747700725")
  void downloadRecordByIdShouldReturnUtfResourceWhenIsUtfIsTrue() {
    // Given
    var recordId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    var idType = IdType.INSTANCE;
    var isUtf = true;
    var suppress999ff = false;
    var expectedResource =
        new InputStreamResource(new ByteArrayInputStream("marc content".getBytes()));
    when(downloadRecordService.processRecordDownload(
            recordId, isUtf, "-utf", idType, suppress999ff))
        .thenReturn(expectedResource);
    // When
    ResponseEntity<Resource> response =
        downloadRecordController.downloadRecordById(recordId, idType, isUtf, suppress999ff);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(expectedResource);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .isEqualTo("attachment; filename=\"550e8400-e29b-41d4-a716-446655440000-utf.mrc\"");
    verify(downloadRecordService)
        .processRecordDownload(recordId, isUtf, "-utf", idType, suppress999ff);
  }
}
