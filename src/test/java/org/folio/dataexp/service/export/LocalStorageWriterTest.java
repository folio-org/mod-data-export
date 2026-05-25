package org.folio.dataexp.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.exception.export.LocalStorageWriterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

class LocalStorageWriterTest {

  @TempDir Path tempDir;

  private LocalStorageWriter createWriter(String fileName) {
    return new LocalStorageWriter(tempDir.resolve(fileName).toString(), OUTPUT_BUFFER_SIZE);
  }

  private Path resolveFile(String fileName) {
    return tempDir.resolve(fileName);
  }

  @Test
  @SneakyThrows
  void writeTest() {
    // Given
    var fileName = "marc.mrc";
    var writer = createWriter(fileName);

    // When
    writer.write("data");
    writer.close();

    // Then
    assertThat(resolveFile(fileName)).exists().isNotEmptyFile();
  }

  @Test
  @SneakyThrows
  void writeIfExceptionTest() {
    // Given
    var fileName = "marc.mrc";
    var writer = createWriter(fileName);

    // When
    writer.write((String) null);
    writer.close();

    // Then
    assertThat(resolveFile(fileName)).doesNotExist();
  }

  @Test
  @TestMate(name = "TestMate-d336844ac6915d74cfdbd10e27afbf4b")
  @SneakyThrows
  void closeShouldCloseWriterWhenFileExists() {
    // Given
    var fileName = "test-file.mrc";
    var localStorageWriter = spy(createWriter(fileName));
    localStorageWriter.write("test data");
    var bufferedWriterSpy =
        spy(
            (BufferedWriter)
                Objects.requireNonNull(ReflectionTestUtils.getField(localStorageWriter, "writer")));
    ReflectionTestUtils.setField(localStorageWriter, "writer", bufferedWriterSpy);
    var filePath = resolveFile(fileName);

    // When
    localStorageWriter.close();

    // Then
    verify(bufferedWriterSpy).close();
    assertThat(filePath).exists().isNotEmptyFile();
  }

  @Test
  @TestMate(name = "TestMate-1467af16f84d16c4f47ba1c056664101")
  @SneakyThrows
  void closeShouldDoNothingWhenFileDoesNotExist() {
    // Given
    var fileName = "test-file.mrc";
    var filePath = resolveFile(fileName);
    var localStorageWriter = createWriter(fileName);
    var bufferedWriterSpy =
        spy(
            (BufferedWriter)
                Objects.requireNonNull(ReflectionTestUtils.getField(localStorageWriter, "writer")));
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
  void closeShouldThrowLocalStorageWriterExceptionWhenWriterFailsToClose() {
    // Given
    var fileName = "test-file.mrc";
    var localStorageWriter = createWriter(fileName);
    var bufferedWriterSpy =
        spy(
            (BufferedWriter)
                Objects.requireNonNull(ReflectionTestUtils.getField(localStorageWriter, "writer")));
    doThrow(new IOException("Simulated I/O error")).when(bufferedWriterSpy).close();
    ReflectionTestUtils.setField(localStorageWriter, "writer", bufferedWriterSpy);

    // When & Then
    assertThatThrownBy(localStorageWriter::close)
        .isInstanceOf(LocalStorageWriterException.class)
        .hasMessage("Error while close(): Simulated I/O error");
    verify(bufferedWriterSpy).close();
  }

  @Test
  @TestMate(name = "TestMate-90b1249644d2bedf44d9e529bb3fa43d")
  @SneakyThrows
  void testConstructorShouldCreateFileAndWriterSuccessfully() {
    // Given
    var fileName = "test-file.mrc";
    var filePath = resolveFile(fileName);

    // When
    var localStorageWriter = createWriter(fileName);

    // Then
    assertThat(filePath).exists();
    localStorageWriter.close();
  }

  @Test
  @TestMate(name = "TestMate-3dad27a304651703987cda0bf7d7e495")
  void testConstructorShouldThrowExceptionWhenPathIsInvalid() {
    // Given
    var invalidPath = "nonexistent_dir/test-file.mrc";

    // When & Then
    assertThatThrownBy(() -> new LocalStorageWriter(invalidPath, OUTPUT_BUFFER_SIZE))
        .isInstanceOf(LocalStorageWriterException.class)
        .hasMessageStartingWith("Files buffer cannot be created due to error: ");
  }

  @Test
  @TestMate(name = "TestMate-5a7cf4941c45955eddb5458d7cfadd55")
  @SneakyThrows
  void testConstructorShouldThrowExceptionWhenFileAlreadyExists() {
    // Given
    var fileName = "existing-file.mrc";
    Files.createFile(resolveFile(fileName));

    // When & Then
    assertThatThrownBy(() -> createWriter(fileName))
        .isInstanceOf(LocalStorageWriterException.class)
        .hasMessageStartingWith("Files buffer cannot be created due to error: ");
  }

  @Test
  @TestMate(name = "TestMate-4f1f712c1d0b083a14e0d33077276a85")
  @SneakyThrows
  void testConstructorShouldThrowExceptionForReadOnlyDirectory() {
    // Given
    var fileName = "test-file.mrc";
    var fileLocation = resolveFile(fileName).toString();
    var tempDirFile = tempDir.toFile();
    tempDirFile.setWritable(false);
    try (var ignored =
        new AutoCloseable() {
          public void close() {
            tempDirFile.setWritable(true);
          }
        }) {

      // When & Then
      assertThatThrownBy(() -> new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE))
          .isInstanceOf(LocalStorageWriterException.class)
          .hasMessageStartingWith("Files buffer cannot be created due to error: ");
    }
  }

  @Test
  @TestMate(name = "TestMate-c585caca90171156d3978648043d35d7")
  @SneakyThrows
  void getReaderShouldReturnBufferedReaderWhenFileExists() {
    // Given
    var fileName = "test-reader.mrc";
    var sampleData = "sample marc data";
    var localStorageWriter = createWriter(fileName);
    localStorageWriter.write(sampleData);
    localStorageWriter.close();

    // When
    Optional<BufferedReader> readerOptional = localStorageWriter.getReader();

    // Then
    assertThat(readerOptional).isPresent();
    try (BufferedReader reader = readerOptional.get()) {
      assertThat(reader.readLine()).isEqualTo(sampleData);
    }
  }

  @Test
  @TestMate(name = "TestMate-696df2b74b62444495b47727c85d5e18")
  @SneakyThrows
  void getReaderShouldReturnEmptyOptionalWhenFileIsMissing() {
    // Given
    var fileName = "missing-file.mrc";
    var filePath = resolveFile(fileName);
    var localStorageWriter = createWriter(fileName);
    Files.delete(filePath);

    // When
    Optional<BufferedReader> readerOptional = localStorageWriter.getReader();

    // Then
    assertThat(readerOptional).isEmpty();
  }

  @Test
  @TestMate(name = "TestMate-85c18e3ebdd59899df406e07e828a1af")
  @SneakyThrows
  void getReaderShouldReturnEmptyOptionalWhenAccessIsDenied() {
    // Given
    var fileName = "restricted-file.mrc";
    var localStorageWriter = createWriter(fileName);
    localStorageWriter.write("restricted data");
    localStorageWriter.close();
    var file = resolveFile(fileName).toFile();
    file.setReadable(false);
    try (var ignored =
        new AutoCloseable() {
          public void close() {
            file.setReadable(true);
          }
        }) {
      // When
      Optional<BufferedReader> readerOptional = localStorageWriter.getReader();

      // Then
      assertThat(readerOptional).isEmpty();
    }
  }

  @Test
  @TestMate(name = "TestMate-c9086cf582de14691d6564478833cb6f")
  void testWriteWhenDataIsEmptyShouldDeleteFile() {
    // Given
    var fileName = "empty_data.mrc";
    var writer = createWriter(fileName);
    var filePath = resolveFile(fileName);
    assertThat(filePath).exists();

    // When
    writer.write("");

    // Then
    assertThat(filePath).doesNotExist();
  }

  @Test
  @TestMate(name = "TestMate-61d3cbd29c6e50d165d9ddf71bb82c45")
  void testWriteWhenDeletionFailsDuringCleanupShouldThrowLocalStorageWriterException() {
    // Given
    var fileName = "cleanup_failure.mrc";
    var writer = createWriter(fileName);
    var filePath = writer.getPath();
    var errorMessage = "Access denied";
    try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
      mockedFiles
          .when(() -> Files.deleteIfExists(filePath))
          .thenThrow(new IOException(errorMessage));

      // When & Then
      assertThatThrownBy(() -> writer.write((String) null))
          .isInstanceOf(LocalStorageWriterException.class)
          .hasMessageStartingWith("Error in deleting file: ")
          .hasMessageContaining(errorMessage);
    }
  }
}
