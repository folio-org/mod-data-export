package org.folio.dataexp.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DataExportServiceIT extends BaseDataExportInitializerIT {

  @MockitoBean private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @MockitoBean private JobProfileEntityRepository jobProfileEntityRepository;
  @MockitoBean private ExportIdEntityRepository exportIdEntityRepository;
  @MockitoBean private InputFileProcessor inputFileProcessor;
  @MockitoBean private SlicerProcessor slicerProcessor;
  @MockitoBean private SingleFileProcessorAsync singleFileProcessorAsync;
  @MockitoBean private UserClient userClient;
  @MockitoBean private DataExportRequestValidator dataExportRequestValidator;
  @MockitoBean private JobExecutionService jobExecutionService;

  @Autowired private DataExportService dataExportService;

  @Test
  @SneakyThrows
  void postDataExport() {
    var userId = UUID.randomUUID();
    var user = new User();
    user.setId(userId.toString());
    var personal = new User.Personal();
    personal.setFirstName("firstName");
    personal.setLastName("lastName");
    user.setPersonal(personal);

    var exportRequest = new ExportRequest();
    exportRequest.setRecordType(ExportRequest.RecordTypeEnum.INSTANCE);
    exportRequest.setJobProfileId(UUID.randomUUID());
    exportRequest.setFileDefinitionId(UUID.randomUUID());

    var fileDefinition =
        new FileDefinition()
            .id(exportRequest.getFileDefinitionId())
            .jobExecutionId(UUID.randomUUID())
            .fileName("instance");
    var fileDefinitionEntity =
        FileDefinitionEntity.builder()
            .fileDefinition(fileDefinition)
            .id(fileDefinition.getId())
            .build();

    var jobProfile =
        new JobProfile()
            .id(exportRequest.getJobProfileId())
            .name("jobProfileName")
            .mappingProfileId(UUID.randomUUID());
    var jobProfileEntity =
        JobProfileEntity.builder().jobProfile(jobProfile).id(jobProfile.getId()).build();

    var jobExecution = new JobExecution().id(fileDefinition.getId());

    when(fileDefinitionEntityRepository.getReferenceById(isA(UUID.class)))
        .thenReturn(fileDefinitionEntity);
    when(jobProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(jobProfileEntity);
    when(jobExecutionService.getById(isA(UUID.class))).thenReturn(jobExecution);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);
    when(jobExecutionService.getNextHrid()).thenReturn(200);
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      dataExportService.postDataExport(exportRequest);
    }
    await()
        .atMost(2, SECONDS)
        .untilAsserted(
            () -> {
              verify(inputFileProcessor)
                  .readFile(
                      eq(fileDefinition),
                      isA(CommonExportStatistic.class),
                      isA(ExportRequest.IdTypeEnum.class));
              verify(slicerProcessor).sliceInstancesIds(fileDefinition, exportRequest);
              verify(singleFileProcessorAsync)
                  .exportBySingleFile(
                      eq(jobExecution.getId()),
                      eq(exportRequest),
                      isA(CommonExportStatistic.class));
              verify(jobExecutionService).getNextHrid();
              verify(jobExecutionService, times(2)).save(isA(JobExecution.class));
              verify(userClient).getUserById(isA(String.class));

              assertEquals(JobExecution.StatusEnum.IN_PROGRESS, jobExecution.getStatus());
              assertEquals(200, jobExecution.getHrId());
              var exportedFiles = jobExecution.getExportedFiles();
              JobExecutionExportedFilesInner inner =
                  (JobExecutionExportedFilesInner) exportedFiles.toArray()[0];
              assertEquals("instance-200.mrc", inner.getFileName());
            });
  }
}
