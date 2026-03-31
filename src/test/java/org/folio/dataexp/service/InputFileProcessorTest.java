package org.folio.dataexp.service;

import org.folio.dataexp.client.SearchClient;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dataexp.util.Constants;
import org.folio.dataexp.util.S3FilePathUtils;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class InputFileProcessorTest {

  @Mock private ExportIdEntityRepository exportIdEntityRepository;
  @Mock private FolioS3Client s3Client;
  @Mock private SearchClient searchClient;
  @Mock private ErrorLogService errorLogService;
  @Mock private JobExecutionService jobExecutionService;
  @Mock private InsertExportIdService insertExportIdService;

  @InjectMocks private InputFileProcessor inputFileProcessor;

    @Test
  void readMarcFile_shouldReturnInputStream_whenFileExistsInS3() {
    // TestMate-a00ce435c0e630fa38fc684ac4eb7c79
    // Given
    var dirName = "job-123";
    var fileName = dirName + "." + Constants.MARC_FILE_SUFFIX;
    var expectedPath = S3FilePathUtils.getPathToStoredRecord(dirName, fileName);
    var expectedStream = new ByteArrayInputStream("marc content".getBytes());
    when(s3Client.list(expectedPath)).thenReturn(List.of(expectedPath));
    when(s3Client.read(expectedPath)).thenReturn(expectedStream);
    // When
    InputStream actualStream = inputFileProcessor.readMarcFile(dirName);
    // Then
    assertThat(actualStream).isSameAs(expectedStream);
    verify(s3Client).list(expectedPath);
    verify(s3Client).read(expectedPath);
  }

    @Test
  void readMarcFile_shouldReturnNull_whenFileDoesNotExistInS3() {
    // TestMate-428ca7bbf86d8824bfafde522242e30b
    // Given
    var dirName = "missing-job";
    var fileName = dirName + "." + Constants.MARC_FILE_SUFFIX;
    var expectedPath = S3FilePathUtils.getPathToStoredRecord(dirName, fileName);
    when(s3Client.list(expectedPath)).thenReturn(Collections.emptyList());
    // When
    InputStream actualStream = inputFileProcessor.readMarcFile(dirName);
    // Then
    assertThat(actualStream).isNull();
    verify(s3Client).list(expectedPath);
    verifyNoMoreInteractions(s3Client);
  }

}
