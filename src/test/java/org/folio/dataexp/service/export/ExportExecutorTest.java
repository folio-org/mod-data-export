package org.folio.dataexp.service.export;

import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.CommonExportFails;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportExecutorTest {
  @Mock
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @Mock
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Mock
  private ExportStrategyFactory exportStrategyFactory;
  @Mock
  private ErrorLogService errorLogService;
  @Mock
  private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  @Mock
  private InstancesExportStrategy instancesExportStrategy;
  @Mock
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock
  private S3ExportsUploader s3ExportsUploader;

  @InjectMocks
  private ExportExecutor exportExecutor;

  @Test
  @SneakyThrows
  void exportTest() {
    var jobExecutionId = UUID.randomUUID();
    var jobExecution = new JobExecution();
    jobExecution.setProgress(new JobExecutionProgress());
    jobExecution.setId(jobExecutionId);
    var jobExecutionEntity = JobExecutionEntity.builder().jobExecution(jobExecution).build();
    var fileDefinition = new FileDefinition();
    fileDefinition.setJobExecutionId(jobExecutionId);
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.setFileName("file_name.csv");
    var fileDefinitionEntity = FileDefinitionEntity.builder().fileDefinition(fileDefinition).id(fileDefinition.getId()).build();

    var fileLocation = String.format("mod-data-export/download/%s/download.mrc", jobExecutionId);
    var exportEntity = JobExecutionExportFilesEntity.builder()
      .id(UUID.randomUUID())
      .jobExecutionId(jobExecutionId)
      .fileLocation(fileLocation).build();

    var completedExportEntity = JobExecutionExportFilesEntity.builder()
      .id(exportEntity.getId())
      .jobExecutionId(jobExecutionId)
      .status(JobExecutionExportFilesStatus.COMPLETED)
      .fileLocation(fileLocation).build();

    var commonFails = new CommonExportFails();

    when(jobExecutionEntityRepository.getReferenceById(jobExecutionId)).thenReturn(jobExecutionEntity);
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntity.getId())).thenReturn(exportEntity);
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(List.of(completedExportEntity));
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(jobExecutionId.toString())).thenReturn(List.of(fileDefinitionEntity));
    when(exportStrategyFactory.getExportStrategy(new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE))).thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveMarcToRemoteStorage(isA(JobExecutionExportFilesEntity.class), isA(ExportRequest.class))).thenReturn(new ExportStrategyStatistic());

    exportExecutor.export(exportEntity, new ExportRequest(), commonFails);

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportEntity.getStatus());
    assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
    verify(s3ExportsUploader).upload(jobExecution, List.of(completedExportEntity), "file_name");
  }

  @Test
  void exportIfCommonFailsExistTest() {
    var jobExecutionId = UUID.randomUUID();
    var jobExecution = new JobExecution();
    jobExecution.setProgress(new JobExecutionProgress());
    jobExecution.setId(jobExecutionId);
    var jobExecutionEntity = JobExecutionEntity.builder().jobExecution(jobExecution).build();
    var fileDefinition = new FileDefinition();
    fileDefinition.setJobExecutionId(jobExecutionId);
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.setFileName("file_name.csv");
    var fileDefinitionEntity = FileDefinitionEntity.builder().fileDefinition(fileDefinition).id(fileDefinition.getId()).build();

    var fileLocation = String.format("mod-data-export/download/%s/download.mrc", jobExecutionId);
    var exportEntity = JobExecutionExportFilesEntity.builder()
      .id(UUID.randomUUID())
      .jobExecutionId(jobExecutionId)
      .fileLocation(fileLocation).build();
    var completedExportEntity = JobExecutionExportFilesEntity.builder()
      .id(exportEntity.getId())
      .jobExecutionId(jobExecutionId)
      .status(JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS)
      .fileLocation(fileLocation).build();

    var commonFails = new CommonExportFails();
    commonFails.incrementDuplicatedUUID();
    commonFails.addToInvalidUUIDFormat("abs");

    when(jobExecutionEntityRepository.getReferenceById(jobExecutionId)).thenReturn(jobExecutionEntity);
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(List.of(completedExportEntity));
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntity.getId())).thenReturn(exportEntity);
    when(exportStrategyFactory.getExportStrategy(new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE))).thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveMarcToRemoteStorage(isA(JobExecutionExportFilesEntity.class), isA(ExportRequest.class))).thenReturn(new ExportStrategyStatistic());
    when(errorLogEntityCqlRepository.countByJobExecutionId(isA(UUID.class))).thenReturn(2l);
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(jobExecutionId.toString())).thenReturn(List.of(fileDefinitionEntity));

    exportExecutor.export(exportEntity, new ExportRequest(), commonFails);

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportEntity.getStatus());
    assertEquals(JobExecution.StatusEnum.COMPLETED_WITH_ERRORS, jobExecution.getStatus());
    verify(errorLogService).saveCommonExportFailsErrors(commonFails, 2, jobExecutionId);
    verify(s3ExportsUploader).upload(jobExecution, List.of(completedExportEntity), "file_name");
  }
}
