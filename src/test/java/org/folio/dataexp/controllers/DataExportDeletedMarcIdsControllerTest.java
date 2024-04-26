package org.folio.dataexp.controllers;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ExportDeletedMarcIdsRequest;
import org.folio.dataexp.service.ExportDeletedMarcIdsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Date;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataExportDeletedMarcIdsControllerTest extends BaseDataExportInitializer {

  @MockBean
  private ExportDeletedMarcIdsService exportDeletedMarcIdsService;

  @Test
  @SneakyThrows
  void postExportDeletedMarcIdsTest() {
    var request = new ExportDeletedMarcIdsRequest();
    request.setFrom(new Date());
    request.setTo(new Date());

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/export-deleted")
        .headers(defaultHeaders())
        .content(asJsonString(request)))
      .andExpect(status().isOk());

    verify(exportDeletedMarcIdsService).postExportDeletedMarcIds(isA(ExportDeletedMarcIdsRequest.class));
  }
}
