package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
public class DataExportServiceTest {

  @Mock
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock
  private JobExecutionEntityRepository jobExecutionEntityRepository;

  @InjectMocks
  private DataExportService dataExportService;

  @Test
  void postFileDefinitionTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("test.csv");

    var fileDefinitionEntity = FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();

    when(fileDefinitionEntityRepository.save(isA(FileDefinitionEntity.class))).thenReturn(fileDefinitionEntity);

    var savedFileDefinition = dataExportService.postFileDefinition(fileDefinition);
    assertEquals(FileDefinition.StatusEnum.NEW, savedFileDefinition.getStatus());
    assertNotNull(savedFileDefinition.getJobExecutionId());

    verify(fileDefinitionEntityRepository).save(isA(FileDefinitionEntity.class));
    verify(jobExecutionEntityRepository).save(isA(JobExecutionEntity.class));
  }
}
