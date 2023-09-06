package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileDefinitionsServiceTest {

  @Mock
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FileDefinitionValidator fileDefinitionValidator;
  @Captor
  private ArgumentCaptor<JobExecutionEntity> jobExecutionEntityCaptor;
  @InjectMocks
  private FileDefinitionsService fileDefinitionsService;

  @Test
  void postFileDefinitionTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");

    var fileDefinitionEntity = FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();

    when(fileDefinitionEntityRepository.save(isA(FileDefinitionEntity.class))).thenReturn(fileDefinitionEntity);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    var savedFileDefinition = fileDefinitionsService.postFileDefinition(fileDefinition);
    assertEquals(FileDefinition.StatusEnum.NEW, savedFileDefinition.getStatus());
    assertNotNull(savedFileDefinition.getJobExecutionId());

    verify(fileDefinitionEntityRepository).save(isA(FileDefinitionEntity.class));
    verify(jobExecutionEntityRepository).save(jobExecutionEntityCaptor.capture());

    var jobExecutionEntity = jobExecutionEntityCaptor.getValue();

    assertEquals(JobExecution.StatusEnum.NEW, jobExecutionEntity.getJobExecution().getStatus());
  }
}
