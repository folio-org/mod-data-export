package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.SearchClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.IdsJob;
import org.folio.dataexp.domain.dto.IdsJobPayload;
import org.folio.dataexp.domain.dto.ResourceIds;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.PathResource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InputFileProcessorTest extends BaseDataExportInitializer {

  private static final String UPLOADED_FILE_PATH_CSV = "src/test/resources/upload.csv";
  private static final String UPLOADED_FILE_PATH_CQL = "src/test/resources/upload.cql";

  @Autowired
  private FolioS3Client s3Client;
  @Autowired
  private InputFileProcessor inputFileProcessor;
  @Autowired
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Autowired
  private ExportIdEntityRepository exportIdEntityRepository;
  @MockBean
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
      var jobExecutionEntity = JobExecutionEntity.builder().id(fileDefinition.getJobExecutionId()).build();
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition, new CommonExportStatistic(), ExportRequest.IdTypeEnum.INSTANCE);
      var total = exportIdEntityRepository.count();
      assertEquals(2, total);
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

    when(searchClient.submitIdsJob(any(IdsJobPayload.class))).thenReturn(new IdsJob().withId(fileDefinition.getJobExecutionId())
      .withStatus(IdsJob.Status.COMPLETED));
    var resourceIds = new ResourceIds().withIds(List.of(
      new ResourceIds.Id().withId(UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd")))).withTotalRecords(1);
    when(searchClient.getResourceIds(any(String.class))).thenReturn(resourceIds);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecutionEntity = JobExecutionEntity.builder().id(fileDefinition.getJobExecutionId()).build();
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition, new CommonExportStatistic(), ExportRequest.IdTypeEnum.INSTANCE);
      var exportIds = exportIdEntityRepository.findAll();

      assertEquals(1, exportIds.size());

      assertEquals(fileDefinition.getJobExecutionId(), exportIds.get(0).getJobExecutionId());
      assertEquals(UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd"), exportIds.get(0).getInstanceId());
    }
  }
}
