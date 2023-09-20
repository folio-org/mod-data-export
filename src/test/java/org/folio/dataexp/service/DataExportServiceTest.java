package org.folio.dataexp.service;

import lombok.SneakyThrows;
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
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportServiceTest {

  @Mock
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Mock
  private JobProfileEntityRepository jobProfileEntityRepository;
  @Mock
  private ExportIdEntityRepository exportIdEntityRepository;
  @Mock
  private InputFileProcessor inputFileProcessor;
  @Mock
  private SlicerProcessor slicerProcessor;
  @Mock
  private SingleFileProcessorAsync singleFileProcessorAsync;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private UserClient userClient;
  @Mock
  private DataExportRequestValidator dataExportRequestValidator;

  @InjectMocks
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
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);


    dataExportService.postDataExport(exportRequest);

    verify(inputFileProcessor).readFile(fileDefinition);
    verify(slicerProcessor).sliceInstancesIds(fileDefinition);

    verify(singleFileProcessorAsync).exportBySingleFile(jobExecution.getId(), ExportRequest.RecordTypeEnum.INSTANCE);
    verify(exportIdEntityRepository).countByJobExecutionId(jobExecution.getId());
    verify(jobExecutionEntityRepository).getHrid();
    verify(jobExecutionEntityRepository).save(isA(JobExecutionEntity.class));

    assertEquals(JobExecution.StatusEnum.IN_PROGRESS, jobExecution.getStatus());
  }
}
