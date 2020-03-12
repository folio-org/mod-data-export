package org.folio.rest.impl;

import io.vertx.core.Future;
import org.folio.rest.exceptions.HttpException;
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

  Future<FileDownload> getDownloadLink(String jobExecutionId, String exportFileId, String tenantId) {
    return getDownloadFileName(jobExecutionId, exportFileId, tenantId)
      .compose(fileName -> exportStorageService.getFileDownloadLink(jobExecutionId, fileName, tenantId))
      .map(link -> new FileDownload().withFileId(exportFileId)
        .withLink(link));
  }

  Future<String> getDownloadFileName(String jobExecutionId, String exportFileId, String tenantId) {
    return jobExecutionService.getById(jobExecutionId, tenantId)
      .map(job -> job.map(jb -> jb.getExportedFiles()
        .stream()
        .filter(expFile -> expFile.getFileId()
          .equals(exportFileId))
        .findFirst())
        .orElseThrow(() -> new HttpException(404, String.format("Job with id: %s not found", jobExecutionId))))
      .map(exportedFile -> exportedFile.map(ExportedFile::getFileName)
          .orElseThrow(() -> new HttpException(404, String.format("Export File with id: %s not found: ", exportFileId))));

  }

}
