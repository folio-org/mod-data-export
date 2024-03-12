package org.folio.dataexp.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.UUID;

class LogsControllerTest extends BaseDataExportInitializer {
  @MockBean
  private ErrorLogEntityCqlRepository repository;

  @Test
  @SneakyThrows
  void getTransformationFieldsTest() {
    var entity = ErrorLogEntity.builder()
      .id(UUID.randomUUID())
      .errorLog(new ErrorLog().id(UUID.randomUUID()))
      .build();

    when(repository.findByCql(anyString(), any(OffsetRequest.class)))
      .thenReturn(new PageImpl<>(Collections.singletonList(entity)));

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/logs?offset=0&limit=1")
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk());
  }
}
