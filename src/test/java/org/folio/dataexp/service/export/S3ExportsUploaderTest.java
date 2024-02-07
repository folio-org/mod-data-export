package org.folio.dataexp.service.export;

import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.folio.dataexp.util.Constants.TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ExportsUploaderTest {

  @Mock
  private FolioS3Client s3Client;

  @InjectMocks
  private S3ExportsUploader s3ExportsUploader;

  @Test
  @SneakyThrows
  void uploadSingleExportsTest() {
    var marc = "marc";
    var initialFileName = "marc_export";
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = String.format(TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var fileLocation = temDirLocation + initialFileName;
    var writer =  new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    writer.write(marc);
    writer.close();
    var export = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation).build();

    var expectedS3Path = temDirLocation + "marc_export-200.mrc";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export), initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadMultipleExportsTest() {
    var marc = "marc";
    var initialFileName = "marc_export";
    var exportFileName1 = "marc_export_sliced_1.mrc";
    var exportFileName2 = "marc_export_sliced_2.mrc";
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = String.format(TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var fileLocation1 = temDirLocation + exportFileName1;
    var writer =  new LocalStorageWriter(fileLocation1, OUTPUT_BUFFER_SIZE);
    writer.write(marc);
    writer.close();

    var fileLocation2 = temDirLocation + exportFileName2;
    writer =  new LocalStorageWriter(fileLocation2, OUTPUT_BUFFER_SIZE);
    writer.write(marc);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var expectedS3Path = temDirLocation + "marc_export.zip";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export1, export2), initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }
}
