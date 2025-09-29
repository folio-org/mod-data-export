package org.folio.dataexp.controllers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class CleanUpFilesControllerTest extends BaseDataExportInitializer {
  @Test
  @SneakyThrows
  void postCleanUpFiles() {
    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/clean-up-files")
        .headers(defaultHeaders()))
        .andExpect(status().isNoContent());
  }
}
