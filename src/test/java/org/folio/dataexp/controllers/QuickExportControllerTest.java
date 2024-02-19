package org.folio.dataexp.controllers;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.QuickExportRequest;
import org.folio.dataexp.service.QuickExportService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuickExportControllerTest extends BaseDataExportInitializer {

  @MockBean
  private QuickExportService quickExportService;

  @ParameterizedTest
  @SneakyThrows
  @ValueSource(strings = {"INSTANCE", "AUTHORITY"})
  void postDataExportTest(String recordType) {
    var quickExportRequest = new QuickExportRequest();
    quickExportRequest.setRecordType(QuickExportRequest.RecordTypeEnum.fromValue(recordType));
    quickExportRequest.setType(QuickExportRequest.TypeEnum.UUID);
    quickExportRequest.setUuids(List.of(UUID.randomUUID()));

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/quick-export")
        .headers(defaultHeaders())
        .content(asJsonString(quickExportRequest)))
      .andExpect(status().isOk());

    verify(quickExportService).postQuickExport(isA(QuickExportRequest.class));
  }
}
