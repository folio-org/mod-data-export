package org.folio.dataexp.service.export.strategies;

import lombok.Getter;
import org.folio.dataexp.repository.JobExecutionEntityRepository;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ExportStrategyStatisticListener {

  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Getter
  private final AtomicInteger exportedCount = new AtomicInteger();
  private int batchSize;
  private UUID jobExecutionId;

  public ExportStrategyStatisticListener(JobExecutionEntityRepository jobExecutionEntityRepository, int batchSize, UUID jobExecutionId ) {
    this.jobExecutionEntityRepository = jobExecutionEntityRepository;
    this.batchSize = batchSize;
    this.jobExecutionId = jobExecutionId;
  }

  public synchronized void incrementExported() {
    var exported = exportedCount.incrementAndGet();
    if (exported % batchSize == 0) {
      var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
      var progress = jobExecutionEntity.getJobExecution().getProgress();
      progress.setExported(exported);
      jobExecutionEntityRepository.save(jobExecutionEntity);
    }
  }
}
