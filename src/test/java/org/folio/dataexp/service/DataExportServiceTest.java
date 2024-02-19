package org.folio.dataexp.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataExportServiceTest extends BaseDataExportInitializer {

  @MockBean
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @MockBean
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @MockBean
  private JobProfileEntityRepository jobProfileEntityRepository;
  @MockBean
  private ExportIdEntityRepository exportIdEntityRepository;
  @MockBean
  private InputFileProcessor inputFileProcessor;
  @MockBean
  private SlicerProcessor slicerProcessor;
  @MockBean
  private SingleFileProcessorAsync singleFileProcessorAsync;
  @MockBean
  private UserClient userClient;
  @MockBean
  private DataExportRequestValidator dataExportRequestValidator;

  @Autowired
  private DataExportService dataExportService;

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

    var fileDefinition = new FileDefinition().id(exportRequest.getFileDefinitionId()).jobExecutionId(UUID.randomUUID());
    var fileDefinitionEntity = FileDefinitionEntity.builder()
      .fileDefinition(fileDefinition).id(fileDefinition.getId()).build();

    var jobProfile = new JobProfile().id(exportRequest.getJobProfileId())
      .name("jobProfileName").mappingProfileId(UUID.randomUUID());
    var jobProfileEntity = JobProfileEntity.builder()
        .jobProfile(jobProfile).id(jobProfile.getId()).build();

    var jobExecution = new JobExecution().id(fileDefinition.getId());
    var jobExecutionEntity = JobExecutionEntity.builder()
        .jobExecution(jobExecution).id(jobExecution.getId()).build();

    when(fileDefinitionEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(fileDefinitionEntity);
    when(jobProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(jobProfileEntity);
    when(jobExecutionEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(jobExecutionEntity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);
    when(jobExecutionEntityRepository.getHrid()).thenReturn(200);
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      dataExportService.postDataExport(exportRequest);
    }
    await().atMost(2, SECONDS).untilAsserted(() -> {
      verify(inputFileProcessor).readFile(eq(fileDefinition), isA(CommonExportFails.class));
      verify(slicerProcessor).sliceInstancesIds(fileDefinition, exportRequest);
      verify(singleFileProcessorAsync).exportBySingleFile(eq(jobExecution.getId()), eq(exportRequest), isA(CommonExportFails.class));
      verify(exportIdEntityRepository, times(2)).countByJobExecutionId(jobExecution.getId());
      verify(jobExecutionEntityRepository).getHrid();
      verify(jobExecutionEntityRepository, times(2)).save(isA(JobExecutionEntity.class));
      verify(userClient).getUserById(isA(String.class));

      assertEquals(JobExecution.StatusEnum.IN_PROGRESS, jobExecution.getStatus());
      assertEquals(200, jobExecution.getHrId());
    });
  }
}
