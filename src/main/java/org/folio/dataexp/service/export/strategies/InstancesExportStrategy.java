package org.folio.dataexp.service.export.strategies;

import lombok.AllArgsConstructor;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.RemoteStorageWriter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InstancesExportStrategy implements ExportStrategy {

  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private final FolioS3Client s3Client;

  @Override
  public ExportStrategyStatistic saveMarcToRemoteStorage(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest,
      boolean lastExport) {
    var marc = "marc";
    try (var remoteStorageWriter = new RemoteStorageWriter(exportFilesEntity.getFileLocation(),  8192, s3Client)) {
      remoteStorageWriter.write(marc);
    }
    var exportStatistic = new ExportStrategyStatistic();
    exportStatistic.setExported(1);
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    return exportStatistic;
  }
}
