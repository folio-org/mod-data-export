package org.folio.dataexp.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.service.DownloadRecordService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class DownloadRecordControllerTest extends BaseDataExportInitializer {

  @MockBean
  private DownloadRecordService downloadRecordService;

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("provideUtfFlags")
  void downloadAuthorityById_whenNoUftProvided(Boolean isUtf) {
    var authorityId = UUID.randomUUID();
    var formatPostfix = Boolean.FALSE.equals(isUtf) ? "-marc8" : "-utf";
    var expectedFileName = authorityId + formatPostfix + ".mrc";
    var mockData = "some data".getBytes();
    var mockResource = new ByteArrayResource(mockData);
    when(downloadRecordService.processRecordDownload(authorityId, isUtf == null || isUtf, formatPostfix, "AUTHORITY"))
      .thenReturn(mockResource);

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/download-record/{recordId}", authorityId)
        .param("utf", isUtf != null ? isUtf.toString() : null)
        .param("idType", "AUTHORITY")
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
      .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + expectedFileName + "\""))
      .andExpect(content().bytes(mockData));

    verify(downloadRecordService).processRecordDownload(authorityId, isUtf == null || isUtf, formatPostfix, "AUTHORITY");
  }

  private static Stream<Boolean> provideUtfFlags() {
    return Stream.of(null, Boolean.TRUE, Boolean.FALSE);
  }
}
