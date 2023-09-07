package org.folio.dataexp.controllers;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.service.DataExportService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DataExportControllerTest extends BaseDataExportInitializer {

  @MockBean
  private DataExportService dataExportService;

  @Test
  @SneakyThrows
  void postDataExportTest() {
    var exportRequest = new ExportRequest();
    exportRequest.setRecordType(ExportRequest.RecordTypeEnum.INSTANCE);
    exportRequest.setJobProfileId(UUID.randomUUID());
    exportRequest.setFileDefinitionId(UUID.randomUUID());

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/export")
        .headers(defaultHeaders())
        .content(asJsonString(exportRequest)))
      .andExpect(status().isOk());

    verify(dataExportService).postDataExport(isA(ExportRequest.class));
  }
}
