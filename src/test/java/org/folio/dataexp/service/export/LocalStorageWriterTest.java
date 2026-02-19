package org.folio.dataexp.service.export;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.exception.export.LocalStorageWriterException;
import org.folio.dataexp.util.S3FilePathUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class LocalStorageWriterTest {

  @Test
  @SneakyThrows
  void writeTest() {
    var jobExecutionId = UUID.randomUUID();
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecutionId);
    Files.createDirectories(Path.of(temDirLocation));
    var fileLocation = temDirLocation + "marc.mrc";

    var writer = new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    writer.write("data");
    writer.close();
    var file = new File(fileLocation);
    assertTrue(file.length() > 0);

    FileUtils.deleteDirectory(new File(temDirLocation));
  }

  @Test
  @SneakyThrows
  void writeIfExceptionTest() {
    String invalidData = null;
    var jobExecutionId = UUID.randomUUID();
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecutionId);
    Files.createDirectories(Path.of(temDirLocation));
    var fileLocation = temDirLocation + "marc.mrc";

    var writer = new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);

    writer.write(invalidData);
    writer.close();
    var file = new File(fileLocation);

    assertFalse(file.exists());

    FileUtils.deleteDirectory(new File(temDirLocation));
  }

  @Test
  @TestMate(name = "TestMate-d336844ac6915d74cfdbd10e27afbf4b")
  @SneakyThrows
  void closeShouldCloseWriterWhenFileExists(@TempDir Path tempDir) {
    // Given
    String fileName = "test-file.mrc";
    Path filePath = tempDir.resolve(fileName);
    String fileLocation = filePath.toString();
    LocalStorageWriter localStorageWriter =
        spy(new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE));
    localStorageWriter.write("test data");
    BufferedWriter bufferedWriterSpy =
        spy((BufferedWriter) ReflectionTestUtils.getField(localStorageWriter, "writer"));
    ReflectionTestUtils.setField(localStorageWriter, "writer", bufferedWriterSpy);
    // When
    localStorageWriter.close();
    // Then
    verify(bufferedWriterSpy).close();
    assertTrue(Files.exists(filePath));
    assertTrue(Files.size(filePath) > 0);
  }

  @Test
  @TestMate(name = "TestMate-1467af16f84d16c4f47ba1c056664101")
  @SneakyThrows
  void closeShouldDoNothingWhenFileDoesNotExist(@TempDir Path tempDir) {
    // Given
    String fileName = "test-file.mrc";
    Path filePath = tempDir.resolve(fileName);
    String fileLocation = filePath.toString();
    LocalStorageWriter localStorageWriter =
        new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    BufferedWriter bufferedWriterSpy =
        spy((BufferedWriter) ReflectionTestUtils.getField(localStorageWriter, "writer"));
    ReflectionTestUtils.setField(localStorageWriter, "writer", bufferedWriterSpy);
    Files.delete(filePath);
    // When
    localStorageWriter.close();
    // Then
    verify(bufferedWriterSpy, never()).close();
  }

  @Test
  @TestMate(name = "TestMate-12593f7e358bbbcb6cb0732ef84f02cd")
  @SneakyThrows
  void closeShouldThrowLocalStorageWriterExceptionWhenWriterFailsToClose(@TempDir Path tempDir) {
    // Given
    String fileName = "test-file.mrc";
    Path filePath = tempDir.resolve(fileName);
    String fileLocation = filePath.toString();
    LocalStorageWriter localStorageWriter =
        new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    BufferedWriter bufferedWriterSpy =
        spy((BufferedWriter) ReflectionTestUtils.getField(localStorageWriter, "writer"));
    doThrow(new IOException("Simulated I/O error")).when(bufferedWriterSpy).close();
    ReflectionTestUtils.setField(localStorageWriter, "writer", bufferedWriterSpy);
    // When
    var exception = assertThrows(LocalStorageWriterException.class, localStorageWriter::close);
    // Then
    assertEquals("Error while close(): Simulated I/O error", exception.getMessage());
    verify(bufferedWriterSpy).close();
  }

  @Test
  @TestMate(name = "TestMate-90b1249644d2bedf44d9e529bb3fa43d")
  @SneakyThrows
  void testConstructorShouldCreateFileAndWriterSuccessfully(@TempDir Path tempDir) {
    // Given
    String fileName = "test-file.mrc";
    Path filePath = tempDir.resolve(fileName);
    String fileLocation = filePath.toString();
    // When
    var localStorageWriter = new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    // Then
    assertTrue(Files.exists(filePath), "File should be created by the constructor.");
    // Closing the writer to release resources, which is a good practice.
    localStorageWriter.close();
  }

  @Test
  @TestMate(name = "TestMate-3dad27a304651703987cda0bf7d7e495")
  void testConstructorShouldThrowExceptionWhenPathIsInvalid() {
    // Given
    String invalidPath = "nonexistent_dir/test-file.mrc";
    // When
    var exception =
        assertThrows(
            LocalStorageWriterException.class,
            () -> new LocalStorageWriter(invalidPath, OUTPUT_BUFFER_SIZE));
    // Then
    assertTrue(
        exception.getMessage().startsWith("Files buffer cannot be created due to error: "),
        "Exception message should indicate a file creation error.");
  }

  @Test
  @TestMate(name = "TestMate-5a7cf4941c45955eddb5458d7cfadd55")
  @SneakyThrows
  void testConstructorShouldThrowExceptionWhenFileAlreadyExists(@TempDir Path tempDir) {
    // Given
    String fileName = "existing-file.mrc";
    Path filePath = tempDir.resolve(fileName);
    Files.createFile(filePath);
    String fileLocation = filePath.toString();
    // When
    var exception =
        assertThrows(
            LocalStorageWriterException.class,
            () -> new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE));
    // Then
    assertTrue(
        exception.getMessage().startsWith("Files buffer cannot be created due to error: "),
        "Exception message should indicate a file creation error.");
  }

  @Test
  @TestMate(name = "TestMate-4f1f712c1d0b083a14e0d33077276a85")
  @SneakyThrows
  void testConstructorShouldThrowExceptionForReadOnlyDirectory(@TempDir Path tempDir) {
    // Given
    String fileName = "test-file.mrc";
    String fileLocation = tempDir.resolve(fileName).toString();
    File tempDirFile = tempDir.toFile();
    tempDirFile.setWritable(false);
    try {
      // When
      var exception =
          assertThrows(
              LocalStorageWriterException.class,
              () -> new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE));
      // Then
      assertTrue(
          exception.getMessage().startsWith("Files buffer cannot be created due to error: "),
          "Exception message should indicate a file creation error due to permissions.");
    } finally {
      // Cleanup
      tempDirFile.setWritable(true);
    }
  }
}
