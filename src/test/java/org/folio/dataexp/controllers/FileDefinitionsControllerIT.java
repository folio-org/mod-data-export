package org.folio.dataexp.controllers;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.service.FileDefinitionsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class FileDefinitionsControllerIT extends BaseDataExportInitializerIT {

  @MockitoBean
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @MockitoBean
  private FileDefinitionsService fileDefinitionsService;

  @Test
  @SneakyThrows
  void postFileDefinitionTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setFileName("upload.csv");
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);

    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/file-definitions")
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(asJsonString(fileDefinition)))
        .andExpect(status().isCreated());
  }

  @Test
  @SneakyThrows
  void getFileDefinitionByIdTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");

    when(fileDefinitionEntityRepository.getReferenceById(fileDefinition.getId()))
        .thenReturn(FileDefinitionEntity.builder().fileDefinition(fileDefinition).build());

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/file-definitions/" + fileDefinition.getId().toString())
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void uploadFileTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");

    when(fileDefinitionEntityRepository.getReferenceById(fileDefinition.getId()))
        .thenReturn(FileDefinitionEntity.builder().fileDefinition(fileDefinition).build());

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/file-definitions/" + fileDefinition.getId().toString()
            + "/upload")
        .headers(defaultHeaders())
        .contentType(APPLICATION_OCTET_STREAM_VALUE)
        .content("uuid"))
        .andExpect(status().isOk());
  }
}
