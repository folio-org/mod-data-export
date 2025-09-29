package org.folio.dataexp.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.Config;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class ConfigurationControllerTest extends BaseDataExportInitializer {

  @Test
  @SneakyThrows
  void postDataExportConfigurationTest() {
    var config = new Config().key("slice_size").value("50000");

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/configuration")
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(asJsonString(config)))
        .andExpect(status().isCreated())
        .andExpect(content().json("{\"key\":\"slice_size\",\"value\":\"50000\"}"));
  }
}
