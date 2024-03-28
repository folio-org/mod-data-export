package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.exception.file.definition.UploadFileException;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.service.file.upload.FilesUploadService;
import org.folio.dataexp.service.validators.FileDefinitionValidator;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.PathResource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileDefinitionsServiceTest {

  @Mock
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FileDefinitionValidator fileDefinitionValidator;
  @Mock
  private FilesUploadService filesUploadService;
  @Captor
  private ArgumentCaptor<JobExecution> jobExecutionArgumentCaptor;
  @InjectMocks
  private FileDefinitionsService fileDefinitionsService;

  @Test
  void postFileDefinitionTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.fileName("upload.csv");

    var fileDefinitionEntity = FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();

    when(jobExecutionService.save(any(JobExecution.class))).thenReturn(new JobExecution().id(UUID.randomUUID()));
    when(fileDefinitionEntityRepository.save(isA(FileDefinitionEntity.class))).thenReturn(fileDefinitionEntity);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    var savedFileDefinition = fileDefinitionsService.postFileDefinition(fileDefinition);
    assertNotNull(savedFileDefinition.getId());
    assertEquals(FileDefinition.StatusEnum.NEW, savedFileDefinition.getStatus());
    assertNotNull(savedFileDefinition.getJobExecutionId());

    verify(fileDefinitionEntityRepository).save(isA(FileDefinitionEntity.class));
    verify(jobExecutionService).save(jobExecutionArgumentCaptor.capture());

    var jobExecution = jobExecutionArgumentCaptor.getValue();

    assertEquals(JobExecution.StatusEnum.NEW, jobExecution.getStatus());
  }

  @Test
  void getFileDefinitionByIdTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");

    var fileDefinitionEntity = FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
    when(fileDefinitionEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(fileDefinitionEntity);

    fileDefinitionsService.getFileDefinitionById(UUID.randomUUID());

    verify(fileDefinitionEntityRepository).getReferenceById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void uploadFileTest() {
    var fileDefinitionId = UUID.randomUUID();
    var resource = new PathResource("src/test/resources/upload.csv");

    fileDefinitionsService.uploadFile(fileDefinitionId, resource);
    verify(filesUploadService).uploadFile(fileDefinitionId, resource);
  }

  @Test
  @SneakyThrows
  void uploadFileIfExceptionTest() {
    var fileDefinitionId = UUID.randomUUID();
    var resource = new PathResource("src/test/resources/upload.csv");

    when(filesUploadService.uploadFile(fileDefinitionId, resource)).thenThrow(new RuntimeException("error"));

    assertThrows(UploadFileException.class, () -> fileDefinitionsService.uploadFile(fileDefinitionId, resource));
  }

}
