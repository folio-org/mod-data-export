package org.folio.dataexp.service.file.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class FilesDownloadServiceTest {

  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private FolioS3Client s3Client;

  @InjectMocks
  private FileDownloadServiceImpl fileDownloadService;

  @Test
  @SneakyThrows
  void downloadFileTest() {

    final String expectedPresignedUrl = "expected-presigned-url";

    var jobExecutionId = UUID.randomUUID();
    var exportFileId = UUID.randomUUID();

    var jobExecution = new JobExecution().exportedFiles(Set.of(new JobExecutionExportedFilesInner().fileId(exportFileId)
        .fileName("file.mrc")));

    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(s3Client.getPresignedUrl(any())).thenReturn(expectedPresignedUrl);

    var fileDownload = fileDownloadService.getFileDownload(jobExecutionId, exportFileId);
    var actualExportedFileId = fileDownload.getFileId();
    var actualPresignedUrl = fileDownload.getLink();

    assertEquals(actualPresignedUrl, expectedPresignedUrl);
    assertEquals(actualExportedFileId, exportFileId);
  }
}
