package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.folio.dataexp.service.export.strategies.ExportStrategy;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class SingleFileProcessor {

  private final ExportStrategy instanceExportStrategy;
  private final FolioS3ClientFactory folioS3ClientFactory;
  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;

  public void exportBySingleFile(List<JobExecutionExportFilesEntity> exports, ExportRequest.RecordTypeEnum recordType) {
    exports.forEach(export -> {
      try {
        createExportFile(export, recordType);
      } catch (IOException e) {
        export.setStatus(JobExecutionExportFilesStatus.FAILED);
        jobExecutionExportFilesEntityRepository.save(export);
        log.error("Error exporting {} for jobExecution {}", export.getFileLocation(), export.getJobExecutionId());
      }
    });
  }

  private void createExportFile(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest.RecordTypeEnum recordType) throws IOException {
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.ACTIVE);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    var file = new File(exportFilesEntity.getFileLocation());
    var parent = file.getParentFile();
    if (!parent.exists()) {
      boolean isCreated = parent.mkdirs();
      if (isCreated) log.info("Create local directories to store {} ", exportFilesEntity.getFileLocation());
    }
    boolean isCreated = file.createNewFile();
    if (isCreated) log.info("Create {} locally", exportFilesEntity.getFileLocation());
    instanceExportStrategy.saveMarc(exportFilesEntity, file);
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
      s3Client.write(exportFilesEntity.getFileLocation(), is);
      log.info("Create {} at remote storage", exportFilesEntity.getFileLocation());
    }
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
  }
}
