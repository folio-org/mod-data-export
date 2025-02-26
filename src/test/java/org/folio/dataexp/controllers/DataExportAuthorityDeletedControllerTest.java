package org.folio.dataexp.controllers;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedRequest;
import org.folio.dataexp.service.ExportAuthorityDeletedService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class DataExportAuthorityDeletedControllerTest extends BaseDataExportInitializer {

  @MockitoBean
  private ExportAuthorityDeletedService exportAuthorityDeletedService;

  @Test
  @SneakyThrows
  void postExportDeletedMarcIdsTest() {
    var request = new ExportAuthorityDeletedRequest();
    request.setLimit(10);
    request.setOffset(0);

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/export-authority-deleted")
        .headers(defaultHeaders())
        .content(asJsonString(request)))
      .andExpect(status().isOk());

    verify(exportAuthorityDeletedService).postExportDeletedAuthority(isA(ExportAuthorityDeletedRequest.class));
  }
}
