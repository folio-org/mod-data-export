package org.folio.dataexp.service.export.strategies;

import lombok.AllArgsConstructor;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.folio.s3.client.RemoteStorageWriter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InstancesExportStrategy implements ExportStrategy {

  private final FolioS3ClientFactory folioS3ClientFactory;
  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;

  @Override
  public ExportStrategyStatistic saveMarcToRemoteStorage(JobExecutionExportFilesEntity exportFilesEntity) {
    var marc = "marc";
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    var remoteStorageWriter = new RemoteStorageWriter(exportFilesEntity.getFileLocation(),  8192, s3Client);
    remoteStorageWriter.write(marc);
    remoteStorageWriter.close();
    var exportStatistic = new ExportStrategyStatistic();
    exportStatistic.setExported(1);
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    return exportStatistic;
  }
}
