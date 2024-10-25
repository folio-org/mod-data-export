package org.folio.dataexp.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.IdType;
import org.folio.dataexp.service.DownloadRecordService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class DownloadRecordControllerTest extends BaseDataExportInitializer {

  @MockBean
  private DownloadRecordService downloadRecordService;

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("providedData")
  void downloadAuthorityById_shouldReturnFile_whenInputDataValid(Boolean isUtf) {
    var authorityId = UUID.randomUUID();
    var formatPostfix = Boolean.FALSE.equals(isUtf) ? "-marc8" : "-utf";
    var expectedFileName = authorityId + formatPostfix + ".mrc";
    var mockData = "some data".getBytes();
    var mockResource = new InputStreamResource(new ByteArrayInputStream(mockData));
    when(downloadRecordService.processRecordDownload(authorityId, isUtf == null || isUtf, formatPostfix, IdType.AUTHORITY))
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

    verify(downloadRecordService).processRecordDownload(authorityId, isUtf == null || isUtf, formatPostfix, IdType.AUTHORITY);
  }

  private static Stream<Arguments> providedData() {
    return Stream.of(
      Arguments.of(null, IdType.AUTHORITY),
      Arguments.of(Boolean.TRUE, IdType.AUTHORITY),
      Arguments.of(Boolean.FALSE, IdType.AUTHORITY),
      Arguments.of(null, IdType.INSTANCE),
      Arguments.of(Boolean.TRUE, IdType.INSTANCE),
      Arguments.of(Boolean.FALSE, IdType.INSTANCE)
    );
  }
}
