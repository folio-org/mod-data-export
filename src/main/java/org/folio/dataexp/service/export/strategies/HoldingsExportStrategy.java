package org.folio.dataexp.service.export.strategies;

import lombok.AllArgsConstructor;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class HoldingsExportStrategy implements ExportStrategy {

  private final FolioS3ClientFactory folioS3ClientFactory;
  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;

  @Override
  public ExportStrategyStatistic saveMarcToRemoteStorage(JobExecutionExportFilesEntity exportFilesEntity) {

    return new ExportStrategyStatistic();
  }
}
