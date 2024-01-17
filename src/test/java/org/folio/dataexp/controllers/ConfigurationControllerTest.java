package org.folio.dataexp.controllers;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.service.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfigurationControllerTest extends BaseDataExportInitializer {

  @MockBean
  private ConfigurationService configurationService;

  @Test
  @SneakyThrows
  void postDataExportConfigurationTest() {
    var config = new Config().key("slice_size").value("50000");

    when(configurationService.upsertConfiguration(isA(Config.class))).thenReturn(config);

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/configuration")
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(asJsonString(config)))
      .andExpect(status().isCreated())
      .andExpect(content().json("{\"key\":\"slice_size\",\"value\":\"50000\"}"));
  }
}
