package org.folio.dataexp.service.export;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.folio.dataexp.BaseTest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExportExecutorTest extends BaseTest {
  @MockBean
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @Autowired
  private FolioS3ClientFactory folioS3ClientFactory;
  @Autowired
  private ExportExecutor exportExecutor;

  @Test
  @SneakyThrows
  void executeTest() {
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    var jobExecutionId = UUID.randomUUID();
    var fileLocation = String.format("mod-data-export/download/%s/download.mrc", jobExecutionId);
    var exportEntity = JobExecutionExportFilesEntity.builder()
      .id(UUID.randomUUID())
      .fileLocation(fileLocation).build();
    var temp = new File("mod-data-export/download/" + jobExecutionId);
    temp.mkdirs();


    exportExecutor.export(exportEntity, ExportRequest.RecordTypeEnum.INSTANCE);

    var expectedFile = new File(fileLocation);
    var path = Paths.get(fileLocation);
    assertTrue(Files.exists(path));

    long size = s3Client.getSize(fileLocation);
    assertTrue(size > 0);

    assertEquals(JobExecutionExportFilesStatus.COMPLETED, exportEntity.getStatus());

    FileUtils.delete(expectedFile);
  }
}
