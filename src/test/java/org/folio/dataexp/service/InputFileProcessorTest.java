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
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.folio.dataexp.util.ErrorCode.ERROR_DUPLICATED_IDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputFileProcessorTest extends BaseDataExportInitializer {

  private static final String UPLOADED_FILE_PATH_CSV = "src/test/resources/upload.csv";
  private static final String UPLOADED_FILE_PATH_WITH_UTF8_BOM_CSV = "src/test/resources/upload_with_bom.csv";
  private static final String UPLOADED_FILE_PATH_FOR_DUPLICATED_CSV = "src/test/resources/upload_duplicated.csv";
  private static final String UPLOADED_FILE_PATH_CQL = "src/test/resources/upload.cql";

  @Autowired
  private FolioS3Client s3Client;
  @Autowired
  private InputFileProcessor inputFileProcessor;
  @Autowired
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Autowired
  private ExportIdEntityRepository exportIdEntityRepository;
  @MockitoBean
  private ErrorLogService errorLogService;
  @MockitoBean
  private SearchClient searchClient;

  @Test
  @SneakyThrows
  void readCsvFileTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    fileDefinition.setJobExecutionId(UUID.randomUUID());

    s3Client.createBucketIfNotExists();

    var path = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());
    var resource = new PathResource(UPLOADED_FILE_PATH_CSV);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecution = JobExecution.builder().id(fileDefinition.getJobExecutionId()).build();
      var jobExecutionProgress = new JobExecutionProgress();
      jobExecution.setProgress(jobExecutionProgress);
      var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition, new CommonExportStatistic(), ExportRequest.IdTypeEnum.INSTANCE);
      var total = exportIdEntityRepository.count();
      assertEquals(2, total);
    }
 }

  @Test
  @SneakyThrows
  void readCsvFileWithUtf8BomTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    fileDefinition.setJobExecutionId(UUID.randomUUID());

    s3Client.createBucketIfNotExists();

    var path = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());
    var resource = new PathResource(UPLOADED_FILE_PATH_WITH_UTF8_BOM_CSV);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecution = JobExecution.builder().id(fileDefinition.getJobExecutionId()).build();
      var jobExecutionProgress = new JobExecutionProgress();
      jobExecution.setProgress(jobExecutionProgress);
      var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition, new CommonExportStatistic(), ExportRequest.IdTypeEnum.INSTANCE);
      var total = exportIdEntityRepository.count();
      assertEquals(2, total);
    }
  }

  @Test
  @SneakyThrows
  void readCsvFileIfDuplicatedTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload_duplicated.csv");
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    fileDefinition.setJobExecutionId(UUID.randomUUID());

    s3Client.createBucketIfNotExists();

    var path = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());
    var resource = new PathResource(UPLOADED_FILE_PATH_FOR_DUPLICATED_CSV);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecution = JobExecution.builder().id(fileDefinition.getJobExecutionId()).build();
      var jobExecutionProgress = new JobExecutionProgress();
      jobExecution.setProgress(jobExecutionProgress);
      var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition, new CommonExportStatistic(), ExportRequest.IdTypeEnum.INSTANCE);

      var total = exportIdEntityRepository.count();
      assertEquals(1, total);

      verify(errorLogService).saveGeneralErrorWithMessageValues(ERROR_DUPLICATED_IDS.getCode(), List.of("019e8aea-212d-4d1d-957d-0abcdd0e9acd", "3"), jobExecution.getId());
    }
  }

  @Test
  @SneakyThrows
  void readCqlFileTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.cql");
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CQL);
    fileDefinition.setJobExecutionId(UUID.randomUUID());

    s3Client.createBucketIfNotExists();

    var path = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());
    var resource = new PathResource(UPLOADED_FILE_PATH_CQL);

    when(searchClient.submitIdsJob(any(IdsJobPayload.class))).thenReturn(new IdsJob().withId(UUID.randomUUID())
      .withStatus(IdsJob.Status.COMPLETED));
    when(searchClient.getJobStatus(anyString())).thenReturn(new IdsJob().withId(UUID.randomUUID())
      .withStatus(IdsJob.Status.COMPLETED));
    var resourceIds = new ResourceIds().withIds(List.of(
      new ResourceIds.Id().withId(UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd")))).withTotalRecords(1);
    when(searchClient.getResourceIds(any(String.class))).thenReturn(resourceIds);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecution = JobExecution.builder().id(fileDefinition.getJobExecutionId()).build();
      jobExecution.setProgress(new JobExecutionProgress());
      var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition, new CommonExportStatistic(), ExportRequest.IdTypeEnum.INSTANCE);
      var exportIds = exportIdEntityRepository.findAll();

      assertEquals(1, exportIds.size());
      assertEquals(fileDefinition.getJobExecutionId(), exportIds.get(0).getJobExecutionId());
      assertEquals(UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd"), exportIds.get(0).getInstanceId());

      jobExecution = jobExecutionEntityRepository.getReferenceById(jobExecutionEntity.getId()).getJobExecution();
      assertEquals(1, jobExecution.getProgress().getTotal());
    }
  }
}
