package org.folio.dataexp.service.export;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.folio.dataexp.service.export.S3ExportsUploader.EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.exception.export.S3ExportsUploadException;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3ExportsUploaderTest {

  private static final String EXPORT_TEMP_STORAGE = "temp";
  @Mock
  private FolioS3Client s3Client;

  @InjectMocks
  private S3ExportsUploader s3ExportsUploader;

  @Test
  @SneakyThrows
  void uploadExportsIfExportsEmptyTest() {
    var initialFileName = "initial";
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);

    S3ExportsUploadException s3Exception = assertThrows(S3ExportsUploadException.class, () ->
        s3ExportsUploader.upload(jobExecution, Collections.emptyList(), initialFileName));
    assertEquals(EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE, s3Exception.getMessage());
  }

  @Test
  @SneakyThrows
  void uploadSingleExportsTest() {
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY,
        jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var initialFileName = "marc_export";
    var fileLocation = temDirLocation + initialFileName + ".mrc";
    var writer =  new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();
    var export = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation).build();

    var expectedS3Path = temDirLocation + "marc_export-200.mrc";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export), initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class), isA(Long.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadSingleExportsTestLinkedData() {
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY,
        jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var initialFileName = "linked_data_export";
    var fileLocation = temDirLocation + initialFileName + ".json";
    var writer =  new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    var ldjson = "[{}]";
    writer.write(ldjson);
    writer.close();
    var export = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation).build();

    var expectedS3Path = temDirLocation + "linked_data_export-200.json";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export), initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class), isA(Long.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadSingleExportsIfTempStorageExistsTest() {
    s3ExportsUploader.setExportTmpStorage(EXPORT_TEMP_STORAGE);
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = S3FilePathUtils.getTempDirForJobExecutionId(EXPORT_TEMP_STORAGE,
        jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var initialFileName = "marc_export";
    var fileLocation = String.format("mod-data-export/download/%s/%s.mrc", jobExecution.getId(),
        initialFileName);
    var writer =  new LocalStorageWriter(S3FilePathUtils.getLocalStorageWriterPath(
        EXPORT_TEMP_STORAGE, fileLocation), OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var export = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation).build();

    var expectedS3Path = "mod-data-export/download/" + jobExecution.getId().toString()
        + "/marc_export-200.mrc";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export), initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class), isA(Long.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadSingleExportsIfEmptyTest() {
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY,
        jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var initialFileName = "marc_export";
    var fileLocation = temDirLocation + initialFileName;
    var writer =  new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    writer.close();
    var export = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation).build();

    S3ExportsUploadException s3Exception = assertThrows(S3ExportsUploadException.class, () ->
        s3ExportsUploader.upload(jobExecution, Collections.singletonList(export),
          initialFileName));
    assertEquals(EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE, s3Exception.getMessage());

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadMultipleExportsTest() {
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY,
        jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation1 = temDirLocation + exportFileName1;
    var writer =  new LocalStorageWriter(fileLocation1, OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var fileLocation2 = temDirLocation + exportFileName2;
    writer =  new LocalStorageWriter(fileLocation2, OUTPUT_BUFFER_SIZE);
    writer.write(marc);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var expectedS3Path = temDirLocation + "marc_export-200.zip";
    var initialFileName = "marc_export";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export1, export2),
        initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class), isA(Long.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadMultipleExportsIfTempStorageExistsTest() {
    s3ExportsUploader.setExportTmpStorage(EXPORT_TEMP_STORAGE);
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = S3FilePathUtils.getTempDirForJobExecutionId(EXPORT_TEMP_STORAGE,
        jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation1 = String.format("mod-data-export/download/%s/%s", jobExecution.getId(),
        exportFileName1);
    var writer =  new LocalStorageWriter(S3FilePathUtils.getLocalStorageWriterPath(
        EXPORT_TEMP_STORAGE, fileLocation1), OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var fileLocation2 = String.format("mod-data-export/download/%s/%s", jobExecution.getId(),
        exportFileName2);
    writer =  new LocalStorageWriter(S3FilePathUtils.getLocalStorageWriterPath(EXPORT_TEMP_STORAGE,
        fileLocation2), OUTPUT_BUFFER_SIZE);
    writer.write(marc);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var expectedS3Path = "mod-data-export/download/" + jobExecution.getId().toString()
        + "/marc_export-200.zip";
    var initialFileName = "marc_export";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export1, export2),
        initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class), isA(Long.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadMultipleExportsIfOnlyOneFileWithDataTest() {
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY,
        jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation1 = temDirLocation + exportFileName1;
    var writer =  new LocalStorageWriter(fileLocation1, OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var fileLocation2 = temDirLocation + exportFileName2;
    writer =  new LocalStorageWriter(fileLocation2, OUTPUT_BUFFER_SIZE);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var expectedS3Path = temDirLocation + "marc_export-200.mrc";
    var initialFileName = "marc_export";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export1, export2),
        initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class), isA(Long.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadMultipleExportsIfAllFilesEmptyTest() {
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation  = S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY,
        jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var fileLocation1 = temDirLocation + exportFileName1;
    var writer =  new LocalStorageWriter(fileLocation1, OUTPUT_BUFFER_SIZE);
    writer.close();

    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation2 = temDirLocation + exportFileName2;
    writer =  new LocalStorageWriter(fileLocation2, OUTPUT_BUFFER_SIZE);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var initialFileName = "marc_export";
    var list = List.of(export1, export2);
    S3ExportsUploadException s3Exception = assertThrows(S3ExportsUploadException.class, () ->
        s3ExportsUploader.upload(jobExecution, list, initialFileName));
    assertEquals(EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE, s3Exception.getMessage());

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }
}
