package org.folio.dataexp.service;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.folio.dataexp.domain.dto.JobExecution.StatusEnum.FAIL;
import static org.folio.dataexp.util.ErrorCode.ERROR_JOB_IS_EXPIRED;

import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.springframework.stereotype.Service;

/** Service for managing job executions and their lifecycle. */
@Service
@RequiredArgsConstructor
public class JobExecutionService {
  private final JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;
  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;

  /**
   * Retrieves a JobExecution by its ID.
   *
   * @param id The job execution UUID.
   * @return The JobExecution.
   */
  public JobExecution getById(UUID id) {
    return jobExecutionEntityCqlRepository.getReferenceById(id).getJobExecution();
  }

  /**
   * Saves a JobExecution to the repository.
   *
   * @param jobExecution The JobExecution to save.
   * @return The saved JobExecution.
   */
  public JobExecution save(JobExecution jobExecution) {
    return jobExecutionEntityCqlRepository
        .save(JobExecutionEntity.fromJobExecution(jobExecution))
        .getJobExecution();
  }

  /**
   * Gets the next HRID value.
   *
   * @return The next HRID integer.
   */
  public int getNextHrid() {
    return jobExecutionEntityRepository.getHrid();
  }

  /** Expires job executions that are older than the configured expiration time. */
  public void expireJobExecutions() {
    setCompletedDateForFailedExecutionsIfRequired();
    var expirationDate = new Date(new Date().getTime() - HOURS.toMillis(1));
    jobExecutionEntityCqlRepository
        .getExpiredJobs(expirationDate)
        .forEach(
            jobExecutionEntity -> {
              var jobExecution = jobExecutionEntity.getJobExecution();
              jobExecution.setStatus(FAIL);
              if (nonNull(jobExecution.getProgress())) {
                jobExecution.setProgress(new JobExecutionProgress().exported(0).total(0).failed(0));
              }
              jobExecution.setCompletedDate(new Date());
              updateErrorLogIfJobIsExpired(jobExecution.getId());
              save(jobExecution);
            });
  }

  /** Sets the completed date for failed executions that do not have it set. */
  void setCompletedDateForFailedExecutionsIfRequired() {
    jobExecutionEntityCqlRepository
        .getFailedExecutionsWithoutCompletedDate()
        .forEach(
            jobExecutionEntity -> {
              var jobExecution = jobExecutionEntity.getJobExecution();
              var completedDate =
                  isNull(jobExecution.getLastUpdatedDate())
                      ? new Date()
                      : jobExecution.getLastUpdatedDate();
              jobExecution.setCompletedDate(completedDate);
              save(jobExecution);
            });
  }

  /**
   * Updates the error log for a job execution if it is expired.
   *
   * @param jobExecutionId The job execution UUID.
   */
  private void updateErrorLogIfJobIsExpired(UUID jobExecutionId) {
    var logEntities = errorLogEntityCqlRepository.getAllByJobExecutionId(jobExecutionId);
    if (!logEntities.isEmpty()) {
      var firstEntity = logEntities.get(0);
      var updatedLog =
          firstEntity
              .getErrorLog()
              .errorMessageCode(ERROR_JOB_IS_EXPIRED.getCode())
              .errorMessageValues(singletonList(ERROR_JOB_IS_EXPIRED.getDescription()));
      errorLogEntityCqlRepository.save(firstEntity.withErrorLog(updatedLog));
      // remove all the rest logs to have only 1 reason: job is expired
      logEntities.stream().skip(1).forEach(errorLogEntityCqlRepository::delete);
    }
  }
}
