package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.SearchClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.IdsJob;
import org.folio.dataexp.domain.dto.IdsJobPayload;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.ResourceIds;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.PathResource;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SlicerProcessorTest extends BaseDataExportInitializer {

  private static final String UPLOADED_FILE_PATH_CQL = "src/test/resources/upload_for_slicer.cql";

  @Autowired
  private FolioS3Client s3Client;
  @Autowired
  private InputFileProcessor inputFileProcessor;
  @Autowired
  private SlicerProcessor slicerProcessor;
  @Autowired
  private ExportIdEntityRepository exportIdEntityRepository;
  @Autowired
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @Autowired
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @MockBean
  private SearchClient searchClient;

  @Test
  @SneakyThrows
  void sliceInstancesIdsTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload_for_slicer.cql");
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CQL);

    var progress = new JobExecutionProgress();
    var jobExecution = JobExecution.builder().id(UUID.randomUUID()).build();
    jobExecution.setProgress(progress);
    fileDefinition.setJobExecutionId(jobExecution.getId());

    s3Client.createBucketIfNotExists();

    var path = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());
    var resource = new PathResource(UPLOADED_FILE_PATH_CQL);

    when(searchClient.submitIdsJob(any(IdsJobPayload.class))).thenReturn(new IdsJob().withId(UUID.randomUUID())
      .withStatus(IdsJob.Status.COMPLETED));
    when(searchClient.getJobStatus(anyString())).thenReturn(new IdsJob().withId(UUID.randomUUID())
      .withStatus(IdsJob.Status.COMPLETED));
    var resourceIds = new ResourceIds().withIds(List.of(
      new ResourceIds.Id().withId(UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd")),
      new ResourceIds.Id().withId(UUID.fromString("011e1aea-111d-4d1d-957d-0abcdd0e9acd")))).withTotalRecords(2);
    when(searchClient.getResourceIds(any(String.class))).thenReturn(resourceIds);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition, new CommonExportStatistic(), ExportRequest.IdTypeEnum.INSTANCE);

      var exportRequest = new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE).all(false);
      slicerProcessor.sliceInstancesIds(fileDefinition, 1, exportRequest);
      var exportFiles = jobExecutionExportFilesEntityRepository.findAll();

      assertEquals(2, exportFiles.size());

      var joExecutionExportFilesEntity = exportFiles.get(0);
      var expectedFileLocation = String.format("mod-data-export/download/%s/upload_for_slicer_011e1aea-111d-4d1d-957d-0abcdd0e9acd_011e1aea-111d-4d1d-957d-0abcdd0e9acd.mrc", fileDefinition.getJobExecutionId());
      var expectedFromUUID = UUID.fromString("011e1aea-111d-4d1d-957d-0abcdd0e9acd");
      var expectedToUUID = UUID.fromString("011e1aea-111d-4d1d-957d-0abcdd0e9acd");
      var expectedStatus = JobExecutionExportFilesStatus.SCHEDULED;

      assertEquals(fileDefinition.getJobExecutionId(), joExecutionExportFilesEntity.getJobExecutionId());
      assertEquals(expectedFileLocation, joExecutionExportFilesEntity.getFileLocation());
      assertEquals(expectedFromUUID, joExecutionExportFilesEntity.getFromId());
      assertEquals(expectedToUUID, joExecutionExportFilesEntity.getToId());
      assertEquals(expectedStatus, joExecutionExportFilesEntity.getStatus());

      joExecutionExportFilesEntity = exportFiles.get(1);
      expectedFileLocation = String.format("mod-data-export/download/%s/upload_for_slicer_011e1aea-222d-4d1d-957d-0abcdd0e9acd_011e1aea-222d-4d1d-957d-0abcdd0e9acd.mrc", fileDefinition.getJobExecutionId());
      expectedFromUUID = UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd");
      expectedToUUID = UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd");
      expectedStatus = JobExecutionExportFilesStatus.SCHEDULED;

      assertEquals(fileDefinition.getJobExecutionId(), joExecutionExportFilesEntity.getJobExecutionId());
      assertEquals(expectedFileLocation, joExecutionExportFilesEntity.getFileLocation());
      assertEquals(expectedFromUUID, joExecutionExportFilesEntity.getFromId());
      assertEquals(expectedToUUID, joExecutionExportFilesEntity.getToId());
      assertEquals(expectedStatus, joExecutionExportFilesEntity.getStatus());

      jobExecutionExportFilesEntityRepository.deleteAll();
      exportFiles = jobExecutionExportFilesEntityRepository.findAll();
      assertEquals(0, exportFiles.size());

      slicerProcessor.sliceInstancesIds(fileDefinition, 2, exportRequest);
      exportFiles = jobExecutionExportFilesEntityRepository.findAll();
      assertEquals(1, exportFiles.size());

      joExecutionExportFilesEntity = exportFiles.get(0);
      expectedFileLocation = String.format("mod-data-export/download/%s/upload_for_slicer_011e1aea-111d-4d1d-957d-0abcdd0e9acd_011e1aea-222d-4d1d-957d-0abcdd0e9acd.mrc", fileDefinition.getJobExecutionId());
      expectedFromUUID = UUID.fromString("011e1aea-111d-4d1d-957d-0abcdd0e9acd");
      expectedToUUID = UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd");
      expectedStatus = JobExecutionExportFilesStatus.SCHEDULED;

      assertEquals(fileDefinition.getJobExecutionId(), joExecutionExportFilesEntity.getJobExecutionId());
      assertEquals(expectedFileLocation, joExecutionExportFilesEntity.getFileLocation());
      assertEquals(expectedFromUUID, joExecutionExportFilesEntity.getFromId());
      assertEquals(expectedToUUID, joExecutionExportFilesEntity.getToId());
      assertEquals(expectedStatus, joExecutionExportFilesEntity.getStatus());

      assertEquals(2, exportIdEntityRepository.getExportIds(fileDefinition.getJobExecutionId(), expectedFromUUID, expectedToUUID, PageRequest.of(0, 10))
        .getContent().size());
     }
  }
}
