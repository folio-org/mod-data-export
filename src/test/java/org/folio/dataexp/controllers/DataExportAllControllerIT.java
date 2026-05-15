package org.folio.dataexp.controllers;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.service.DataExportAllService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class DataExportAllControllerIT extends BaseDataExportInitializerIT {

  @MockitoBean private DataExportAllService dataExportAllService;

  @Test
  @SneakyThrows
  void postDataExportTest() {
    var exportAllRequest = new ExportAllRequest();
    exportAllRequest.setIdType(ExportAllRequest.IdTypeEnum.INSTANCE);
    exportAllRequest.setJobProfileId(UUID.randomUUID());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/data-export/export-all")
                .headers(defaultHeaders())
                .content(asJsonString(exportAllRequest)))
        .andExpect(status().isOk());

    verify(dataExportAllService).postDataExportAll(isA(ExportAllRequest.class));
  }
}
