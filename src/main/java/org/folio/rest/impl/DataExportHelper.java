package org.folio.rest.impl;

import io.vertx.core.Future;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDownload;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.job.JobExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataExportHelper {

  @Autowired
  private ExportStorageService exportStorageService;
  @Autowired
  private JobExecutionService jobExecutionService;

  public Future<FileDownload> getDownloadLink(String jobExecutionId, String exportFileId, String tenantId) {
    return getDownloadFileName(jobExecutionId, exportFileId, tenantId)
      .compose(fileName -> exportStorageService.getFileDownloadLink(jobExecutionId, fileName, tenantId))
      .map(link -> new FileDownload()
        .withFileId(exportFileId)
        .withLink(link));
  }

  private Future<String> getDownloadFileName(String jobExecutionId, String exportFileId, String tenantId) {
    return jobExecutionService.getById(jobExecutionId, tenantId)
      .map(job -> job.getExportedFiles()
        .stream()
        .filter(expFile -> expFile.getFileId()
          .equals(exportFileId))
        .findFirst()
        .orElseThrow(() -> new ServiceException(HttpStatus.HTTP_NOT_FOUND, String.format("Export File with id: %s not found:", exportFileId))))
      .map(ExportedFile::getFileName);
  }

}
