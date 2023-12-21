package org.folio.dataexp.service.file.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
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
  private JobExecutionEntityRepository jobExecutionEntityRepository;
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

    var job = new JobExecutionEntity()
      .withJobExecution(new JobExecution().exportedFiles(Set.of(new JobExecutionExportedFilesInner().fileId(exportFileId)
        .fileName("file.mrc"))));

    when(jobExecutionEntityRepository.getReferenceById(jobExecutionId)).thenReturn(job);
    when(s3Client.getPresignedUrl(any())).thenReturn(expectedPresignedUrl);

    var fileDownload = fileDownloadService.getFileDownload(jobExecutionId, exportFileId);
    var actualExportedFileId = fileDownload.getFileId();
    var actualPresignedUrl = fileDownload.getLink();

    assertEquals(actualPresignedUrl, expectedPresignedUrl);
    assertEquals(actualExportedFileId, exportFileId);
  }
}
