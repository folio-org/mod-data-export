package org.folio.dataexp.service.export.strategies;

import lombok.Getter;
import org.folio.dataexp.repository.JobExecutionEntityRepository;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ExportedMarcListener {

  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Getter
  private final AtomicInteger exportedCount = new AtomicInteger();
  private int progressExportedUpdateStep;
  private UUID jobExecutionId;

  public ExportedMarcListener(JobExecutionEntityRepository jobExecutionEntityRepository, int progressExportedUpdateStep, UUID jobExecutionId ) {
    this.jobExecutionEntityRepository = jobExecutionEntityRepository;
    this.progressExportedUpdateStep = progressExportedUpdateStep;
    this.jobExecutionId = jobExecutionId;
  }

  public synchronized void incrementExported() {
    var exported = exportedCount.incrementAndGet();
    if (exported % progressExportedUpdateStep == 0) {
      var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
      var progress = jobExecutionEntity.getJobExecution().getProgress();
      progress.setExported(exported);
      jobExecutionEntityRepository.save(jobExecutionEntity);
    }
  }

  public void removeExported(int exported) {
    exportedCount.getAndAdd(-1 * exported);
  }
}
