package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseTest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;

import java.util.UUID;

import static org.folio.dataexp.service.file.upload.FileUploadServiceImpl.PATTERN_TO_SAVE_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InputFileProcessorTest extends BaseTest {

  private static final String UPLOADED_FILE_PATH_CSV = "src/test/resources/upload.csv";
  private static final String UPLOADED_FILE_PATH_CQL = "src/test/resources/upload.cql";

  @Autowired
  private FolioS3ClientFactory folioS3ClientFactory;
  @Autowired
  private InputFileProcessor inputFileProcessor;
  @Autowired
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Autowired
  private ExportIdEntityRepository exportIdEntityRepository;

  @Test
  @SneakyThrows
  void readCsvFileTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    fileDefinition.setJobExecutionId(UUID.randomUUID());

    var s3Client = folioS3ClientFactory.getFolioS3Client();
    s3Client.createBucketIfNotExists();

    var path = String.format(PATTERN_TO_SAVE_FILE, fileDefinition.getId(), fileDefinition.getFileName());
    var resource = new PathResource(UPLOADED_FILE_PATH_CSV);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecutionEntity = JobExecutionEntity.builder().id(fileDefinition.getJobExecutionId()).build();
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition);
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

    var s3Client = folioS3ClientFactory.getFolioS3Client();
    s3Client.createBucketIfNotExists();

    var path = String.format(PATTERN_TO_SAVE_FILE, fileDefinition.getId(), fileDefinition.getFileName());
    var resource = new PathResource(UPLOADED_FILE_PATH_CQL);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecutionEntity = JobExecutionEntity.builder().id(fileDefinition.getJobExecutionId()).build();
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readFile(fileDefinition);
      var exportIds = exportIdEntityRepository.findAll();

      assertEquals(1, exportIds.size());

      assertEquals(fileDefinition.getJobExecutionId(), exportIds.get(0).getJobExecutionId());
      assertEquals(UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd"), exportIds.get(0).getInstanceId());
    }
  }
}
