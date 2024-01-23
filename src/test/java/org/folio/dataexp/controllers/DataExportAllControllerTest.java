package org.folio.dataexp.controllers;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.service.DataExportAllService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataExportAllControllerTest extends BaseDataExportInitializer {

  @MockBean
  private DataExportAllService dataExportAllService;

  @Test
  @SneakyThrows
  void postDataExportTest() {
    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.INSTANCE);
    exportRequest.setAll(true);
    exportRequest.setJobProfileId(UUID.randomUUID());

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/export-all")
        .headers(defaultHeaders())
        .content(asJsonString(exportRequest)))
      .andExpect(status().isOk());

    verify(dataExportAllService).postDataExportAll(isA(ExportRequest.class));
  }
}
