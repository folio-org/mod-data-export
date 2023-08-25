package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseTest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;

import java.io.IOException;
import java.util.UUID;

import static org.folio.dataexp.service.file.upload.FileUploadServiceImpl.PATTERN_TO_SAVE_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SlicerProcessorTest extends BaseTest {

  private static final String UPLOADED_FILE_PATH_CQL = "src/test/resources/upload.cql";

  @Autowired
  private FolioS3ClientFactory folioS3ClientFactory;
  @Autowired
  private InputFileProcessor inputFileProcessor;
  @Autowired
  private SlicerProcessor slicerProcessor;

  @Autowired
  private JobExecutionExportFilesRepository jobExecutionExportFilesRepository;
  @Autowired
  private JobExecutionEntityRepository jobExecutionEntityRepository;

  @Test
  @SneakyThrows
  public void sliceInstancesIdsTest() {
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.cql");
    fileDefinition.setJobExecutionId(UUID.randomUUID());

    var s3Client = folioS3ClientFactory.getFolioS3Client();
    s3Client.createBucketIfNotExists();

    var path = String.format(PATTERN_TO_SAVE_FILE, fileDefinition.getId(), fileDefinition.getFileName());
    var resource = new PathResource(UPLOADED_FILE_PATH_CQL);

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobExecutionEntity = JobExecutionEntity.builder().id(fileDefinition.getJobExecutionId()).build();
      jobExecutionEntityRepository.save(jobExecutionEntity);
      s3Client.write(path, resource.getInputStream());
      inputFileProcessor.readCqlFile(fileDefinition);

      slicerProcessor.sliceInstancesIds(fileDefinition);
      var exportFiles = jobExecutionExportFilesRepository.findAll();

      assertEquals(1, exportFiles.size());

      var joExecutionExportFilesEntity = exportFiles.get(0);
      var expectedFileLocation = String.format("mod-data-export/download/%s/upload_011e1aea-222d-4d1d-957d-0abcdd0e9acd_011e1aea-222d-4d1d-957d-0abcdd0e9acd.mrc", fileDefinition.getJobExecutionId());
      var expectedFromUUID = UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd");
      var expectedToUUID = UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd");
      var expectedStatus = "SCHEDULED";

      assertEquals(fileDefinition.getJobExecutionId(), joExecutionExportFilesEntity.getJobExecutionId());
      assertEquals(expectedFileLocation, joExecutionExportFilesEntity.getFileLocation());
      assertEquals(expectedFromUUID, joExecutionExportFilesEntity.getFromId());
      assertEquals(expectedToUUID, joExecutionExportFilesEntity.getToId());
      assertEquals(expectedStatus, joExecutionExportFilesEntity.getStatus());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
