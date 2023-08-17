package org.folio.dataexp.controllers;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseTest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DataExportControllerTest extends BaseTest {

  @MockBean
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;

  @Test
  @SneakyThrows
  void postFileDefinitionTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setFileName("test.csv");
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/file-definitions")
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(asJsonString(fileDefinition)))
      .andExpect(status().isCreated());
    fileDefinitionEntityRepository.getReferenceById(fileDefinition.getId());
  }

  @Test
  @SneakyThrows
  void getFileDefinitionByIdTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("test.csv");

    when(fileDefinitionEntityRepository.getReferenceById(eq(fileDefinition.getId())))
      .thenReturn(FileDefinitionEntity.builder().fileDefinition(fileDefinition).build());

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/file-definitions/" + fileDefinition.getId().toString())
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk());
  }
}
