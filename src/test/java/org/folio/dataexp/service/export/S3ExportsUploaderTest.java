package org.folio.dataexp.service.export;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.folio.dataexp.service.export.S3ExportsUploader.EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.exception.export.S3ExportsUploadException;
import org.folio.dataexp.util.Constants;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3ExportsUploaderTest {

  private static final String EXPORT_TEMP_STORAGE = "temp";
  @Mock private FolioS3Client s3Client;

  @InjectMocks private S3ExportsUploader s3ExportsUploader;

  @Test
  @SneakyThrows
  void uploadExportsIfExportsEmptyTest() {
    var initialFileName = "initial";
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);

    S3ExportsUploadException s3Exception =
        assertThrows(
            S3ExportsUploadException.class,
            () -> s3ExportsUploader.upload(jobExecution, Collections.emptyList(), initialFileName));
    assertEquals(EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE, s3Exception.getMessage());
  }

  @Test
  @SneakyThrows
  void uploadSingleExportsTest() {
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var initialFileName = "marc_export";
    var fileLocation = temDirLocation + initialFileName + ".mrc";
    var writer = new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
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
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var initialFileName = "linked_data_export";
    var fileLocation = temDirLocation + initialFileName + ".json";
    var writer = new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
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
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(EXPORT_TEMP_STORAGE, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var initialFileName = "marc_export";
    var fileLocation =
        String.format("mod-data-export/download/%s/%s.mrc", jobExecution.getId(), initialFileName);
    var writer =
        new LocalStorageWriter(
            S3FilePathUtils.getLocalStorageWriterPath(EXPORT_TEMP_STORAGE, fileLocation),
            OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var export = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation).build();

    var expectedS3Path =
        "mod-data-export/download/" + jobExecution.getId().toString() + "/marc_export-200.mrc";
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
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var initialFileName = "marc_export";
    var fileLocation = temDirLocation + initialFileName;
    var writer = new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    writer.close();
    var export = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation).build();

    S3ExportsUploadException s3Exception =
        assertThrows(
            S3ExportsUploadException.class,
            () ->
                s3ExportsUploader.upload(
                    jobExecution, Collections.singletonList(export), initialFileName));
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
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation1 = temDirLocation + exportFileName1;
    var writer = new LocalStorageWriter(fileLocation1, OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var fileLocation2 = temDirLocation + exportFileName2;
    writer = new LocalStorageWriter(fileLocation2, OUTPUT_BUFFER_SIZE);
    writer.write(marc);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var expectedS3Path = temDirLocation + "marc_export-200.zip";
    var initialFileName = "marc_export";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export1, export2), initialFileName);
    assertEquals(expectedS3Path, s3Path);

    verify(s3Client).write(eq(expectedS3Path), isA(InputStream.class), isA(Long.class));

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @SneakyThrows
  void uploadMultipleExportsCheckMemberFileNames() {
    // Capture the zip file before it's deleted from local storage to check its contents
    var uploadedZipFile = new ByteArrayOutputStream();
    when(s3Client.write(anyString(), any(InputStream.class), anyLong()))
      .thenAnswer(invocation -> {
        try (InputStream is = invocation.getArgument(1)) {
          is.transferTo(uploadedZipFile);
        }
        return "some-path";
      });

    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var tempDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecution.getId());
    Files.createDirectories(Path.of(tempDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation1 = tempDirLocation + exportFileName1;
    var writer = new LocalStorageWriter(fileLocation1, OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var fileLocation2 = tempDirLocation + exportFileName2;
    writer = new LocalStorageWriter(fileLocation2, OUTPUT_BUFFER_SIZE);
    writer.write(marc);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var expectedS3Path = tempDirLocation + "marc_export-200.zip";
    var initialFileName = "marc_export";
    var s3PathName = s3ExportsUploader.upload(jobExecution, List.of(export1, export2), initialFileName);
    assertEquals(expectedS3Path, s3PathName);

    try(ZipInputStream zis = new ZipInputStream(
      new ByteArrayInputStream(uploadedZipFile.toByteArray())
    )) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        var name = entry.getName();
        assertTrue(name.endsWith(".mrc"));
      }
    }
  }

  @Test
  @SneakyThrows
  void uploadMultipleExportsIfTempStorageExistsTest() {
    s3ExportsUploader.setExportTmpStorage(EXPORT_TEMP_STORAGE);
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setHrId(200);
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(EXPORT_TEMP_STORAGE, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation1 =
        String.format("mod-data-export/download/%s/%s", jobExecution.getId(), exportFileName1);
    var writer =
        new LocalStorageWriter(
            S3FilePathUtils.getLocalStorageWriterPath(EXPORT_TEMP_STORAGE, fileLocation1),
            OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var fileLocation2 =
        String.format("mod-data-export/download/%s/%s", jobExecution.getId(), exportFileName2);
    writer =
        new LocalStorageWriter(
            S3FilePathUtils.getLocalStorageWriterPath(EXPORT_TEMP_STORAGE, fileLocation2),
            OUTPUT_BUFFER_SIZE);
    writer.write(marc);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var expectedS3Path =
        "mod-data-export/download/" + jobExecution.getId().toString() + "/marc_export-200.zip";
    var initialFileName = "marc_export";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export1, export2), initialFileName);
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
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation1 = temDirLocation + exportFileName1;
    var writer = new LocalStorageWriter(fileLocation1, OUTPUT_BUFFER_SIZE);
    var marc = "marc";
    writer.write(marc);
    writer.close();

    var fileLocation2 = temDirLocation + exportFileName2;
    writer = new LocalStorageWriter(fileLocation2, OUTPUT_BUFFER_SIZE);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var expectedS3Path = temDirLocation + "marc_export-200.mrc";
    var initialFileName = "marc_export";
    var s3Path = s3ExportsUploader.upload(jobExecution, List.of(export1, export2), initialFileName);
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
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecution.getId());
    Files.createDirectories(Path.of(temDirLocation));

    var exportFileName1 = "marc_export_sliced_1.mrc";
    var fileLocation1 = temDirLocation + exportFileName1;
    var writer = new LocalStorageWriter(fileLocation1, OUTPUT_BUFFER_SIZE);
    writer.close();

    var exportFileName2 = "marc_export_sliced_2.mrc";
    var fileLocation2 = temDirLocation + exportFileName2;
    writer = new LocalStorageWriter(fileLocation2, OUTPUT_BUFFER_SIZE);
    writer.close();

    var export1 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation1).build();
    var export2 = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation2).build();

    var initialFileName = "marc_export";
    var list = List.of(export1, export2);
    S3ExportsUploadException s3Exception =
        assertThrows(
            S3ExportsUploadException.class,
            () -> s3ExportsUploader.upload(jobExecution, list, initialFileName));
    assertEquals(EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE, s3Exception.getMessage());

    var temDir = new File(temDirLocation);
    assertFalse(temDir.exists());
  }

  @Test
  @TestMate(name = "TestMate-74dd7dd1ec3da45673b268277e467358")
  @SneakyThrows
  void upload_whenIOExceptionOccurs_shouldThrowS3ExportsUploadException(@TempDir Path tempDir) {
    // Given
    s3ExportsUploader.setExportTmpStorage(tempDir.toString());
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.fromString("a0146319-9786-4632-91c8-a249929828ec"));
    jobExecution.setHrId(200);
    var initialFileName = "io-error-test";
    var relativePath =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecution.getId());
    var fileLocation = relativePath + initialFileName + ".mrc";
    var fullPath =
        Path.of(S3FilePathUtils.getLocalStorageWriterPath(tempDir.toString(), fileLocation));
    Files.createDirectories(fullPath.getParent());
    Files.writeString(fullPath, "some content");
    var exportEntity = JobExecutionExportFilesEntity.builder().fileLocation(fileLocation).build();
    // Simulate IOException by making the file unreadable
    assertTrue(fullPath.toFile().setReadable(false), "Failed to make file unreadable");
    // When & Then
    var exportEntities = Collections.singletonList(exportEntity);
    var exception =
        assertThrows(
            S3ExportsUploadException.class,
            () -> s3ExportsUploader.upload(jobExecution, exportEntities, initialFileName));
    // The exception message for access denied can vary across operating systems.
    String message = exception.getMessage().toLowerCase();
    assertTrue(message.contains("access is denied") || message.contains("permission denied"));
    // Restore permissions to allow cleanup
    fullPath.toFile().setReadable(true);
    // The method under test does not clean up the directory on this specific failure path,
    // so we manually clean it up to not interfere with other tests.
    FileUtils.deleteDirectory(fullPath.getParent().toFile());
  }

  @Test
  @TestMate(name = "TestMate-5a7df17447b9db32b8372f1e82b97c7b")
  @SneakyThrows
  void uploadSingleRecordById_shouldPropagateIOException_whenS3ClientThrowsException() {
    // Given
    var dirName = "failure-case";
    var marcFileContentBytes = "some-marc-data".getBytes();
    var s3FileName = "%s.%s".formatted(dirName, Constants.MARC_FILE_SUFFIX);
    var expectedS3Path = S3FilePathUtils.getPathToStoredRecord(dirName, s3FileName);
    var s3Exception = new RuntimeException("S3 write failed");
    doThrow(s3Exception).when(s3Client).write(eq(expectedS3Path), any(InputStream.class));
    // When & Then
    var thrownException =
        assertThrows(
            RuntimeException.class,
            () -> s3ExportsUploader.uploadSingleRecordById(dirName, marcFileContentBytes));
    assertEquals("S3 write failed", thrownException.getMessage());
    verify(s3Client).write(eq(expectedS3Path), any(InputStream.class));
  }
}
