package org.folio.dataexp.service.export.strategies;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import org.folio.dataexp.repository.JobExecutionEntityRepository;

/**
 * Listener for tracking the number of exported records and updating job execution progress.
 */
public class ExportedRecordsListener {

  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Getter
  private final AtomicInteger exportedCount = new AtomicInteger();
  private int progressExportedUpdateStep;
  private UUID jobExecutionId;

  /**
   * Constructs an ExportedRecordsListener.
   *
   * @param jobExecutionEntityRepository repository for job execution entities
   * @param progressExportedUpdateStep step size for progress updates
   * @param jobExecutionId job execution ID
   */
  public ExportedRecordsListener(
      JobExecutionEntityRepository jobExecutionEntityRepository,
      int progressExportedUpdateStep,
      UUID jobExecutionId
  ) {
    this.jobExecutionEntityRepository = jobExecutionEntityRepository;
    this.progressExportedUpdateStep = progressExportedUpdateStep;
    this.jobExecutionId = jobExecutionId;
  }

  /**
   * Increments the exported count and updates job execution progress if needed.
   */
  public synchronized void incrementExported() {
    var exported = exportedCount.incrementAndGet();
    if (exported % progressExportedUpdateStep == 0) {
      var jobExecutionEntity =
          jobExecutionEntityRepository.getReferenceById(jobExecutionId);
      var progress = jobExecutionEntity.getJobExecution().getProgress();
      progress.setExported(exported);
      jobExecutionEntityRepository.save(jobExecutionEntity);
    }
  }

  /**
   * Removes exported count by the given amount.
   *
   * @param exported number to remove
   */
  public void removeExported(int exported) {
    exportedCount.getAndAdd(-1 * exported);
  }
}
