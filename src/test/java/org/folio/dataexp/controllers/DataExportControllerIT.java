package org.folio.dataexp.controllers;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.service.DataExportService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class DataExportControllerIT extends BaseDataExportInitializerIT {

  @MockitoBean private DataExportService dataExportService;

  @Test
  @SneakyThrows
  void postDataExportTest() {
    var exportRequest = new ExportRequest();
    exportRequest.setRecordType(ExportRequest.RecordTypeEnum.INSTANCE);
    exportRequest.setJobProfileId(UUID.randomUUID());
    exportRequest.setFileDefinitionId(UUID.randomUUID());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/data-export/export")
                .headers(defaultHeaders())
                .content(asJsonString(exportRequest)))
        .andExpect(status().isNoContent());

    verify(dataExportService).postDataExport(isA(ExportRequest.class));
  }
}
