package org.folio.dataexp.service.export;

import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.CommonExportStatistic;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.StorageCleanUpService;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.folio.dataexp.service.export.strategies.ExportedMarcListener;
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
  private JobExecutionService jobExecutionService;
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
  @Mock
  private StorageCleanUpService storageCleanUpService;

  @InjectMocks
  private ExportExecutor exportExecutor;

  @Test
  @SneakyThrows
  void exportTest() {
    var jobExecutionId = UUID.randomUUID();
    var jobExecution = new JobExecution();
    jobExecution.setProgress(new JobExecutionProgress());
    jobExecution.setId(jobExecutionId);
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

    var commonExportStatistic = new CommonExportStatistic();
    commonExportStatistic.setExportedMarcListener(new ExportedMarcListener(null, 1000, null));

    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntity.getId())).thenReturn(exportEntity);
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(List.of(completedExportEntity));
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(jobExecutionId.toString())).thenReturn(List.of(fileDefinitionEntity));
    when(exportStrategyFactory.getExportStrategy(new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE))).thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveMarcToLocalStorage(isA(JobExecutionExportFilesEntity.class), isA(ExportRequest.class), isA(ExportedMarcListener.class))).thenReturn(new ExportStrategyStatistic(new ExportedMarcListener(null, 1000, null)));

    exportExecutor.export(exportEntity, new ExportRequest(), commonExportStatistic);

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportEntity.getStatus());
    assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
    verify(s3ExportsUploader).upload(jobExecution, List.of(completedExportEntity), "file_name");
    verify(storageCleanUpService).cleanExportIdEntities(jobExecution.getId());
  }

  @Test
  void exportIfCommonFailsExistTest() {
    var jobExecutionId = UUID.randomUUID();
    var jobExecution = new JobExecution();
    jobExecution.setProgress(new JobExecutionProgress());
    jobExecution.setId(jobExecutionId);
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

    var commonExportStatistic = new CommonExportStatistic();
    commonExportStatistic.incrementDuplicatedUUID();
    commonExportStatistic.addToInvalidUUIDFormat("abs");
    commonExportStatistic.setExportedMarcListener(new ExportedMarcListener(null, 1000, null));

    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(List.of(completedExportEntity));
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntity.getId())).thenReturn(exportEntity);
    when(exportStrategyFactory.getExportStrategy(new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE))).thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveMarcToLocalStorage(isA(JobExecutionExportFilesEntity.class), isA(ExportRequest.class), isA(ExportedMarcListener.class))).thenReturn(new ExportStrategyStatistic(new ExportedMarcListener(null, 1000, null)));
    when(errorLogEntityCqlRepository.countByJobExecutionId(isA(UUID.class))).thenReturn(2l);
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(jobExecutionId.toString())).thenReturn(List.of(fileDefinitionEntity));

    exportExecutor.export(exportEntity, new ExportRequest(), commonExportStatistic) ;

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportEntity.getStatus());
    assertEquals(JobExecution.StatusEnum.COMPLETED_WITH_ERRORS, jobExecution.getStatus());
    verify(errorLogService).saveCommonExportFailsErrors(commonExportStatistic, 2, jobExecutionId);
    verify(s3ExportsUploader).upload(jobExecution, List.of(completedExportEntity), "file_name");
    verify(storageCleanUpService).cleanExportIdEntities(jobExecution.getId());
  }
}
