package org.folio.dataexp.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.TransformationField;
import org.folio.dataexp.domain.dto.TransformationFieldCollection;
import org.folio.dataexp.service.transformationfields.TransformationFieldsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class TransformationFieldsControllerTest extends BaseDataExportInitializer {
  @MockitoBean private TransformationFieldsService service;

  @Test
  @SneakyThrows
  void getTransformationFieldsTest() {
    when(service.getTransformationFields())
        .thenReturn(
            new TransformationFieldCollection()
                .transformationFields(Collections.singletonList(new TransformationField()))
                .totalRecords(1));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/data-export/transformation-fields")
                .headers(defaultHeaders())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk());
  }
}
