package org.folio.dataexp.service;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.folio.dataexp.domain.dto.JobExecution.StatusEnum.FAIL;
import static org.folio.dataexp.util.ErrorCode.ERROR_JOB_IS_EXPIRED;

import lombok.RequiredArgsConstructor;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobExecutionService {
  private final JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;
  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;

  public void expireJobExecutions() {
    setCompletedDateForFailedExecutionsIfRequired();
    var expirationDate = new Date(new Date().getTime() - HOURS.toMillis(1));
    jobExecutionEntityCqlRepository.getExpiredJobs(expirationDate)
      .forEach(jobExecutionEntity -> {
        var jobExecution = jobExecutionEntity.getJobExecution();
        jobExecution.setStatus(FAIL);
        if (nonNull(jobExecution.getProgress())) {
          jobExecution.setProgress(new JobExecutionProgress().exported(0).total(0).failed(0));
        }
        jobExecution.setCompletedDate(new Date());
        updateErrorLogIfJobIsExpired(jobExecution.getId());
        jobExecutionEntity.setStatus(FAIL);
        jobExecutionEntity.setCompletedDate(jobExecution.getCompletedDate());
        jobExecutionEntityCqlRepository.save(jobExecutionEntity);
      });
  }

  void setCompletedDateForFailedExecutionsIfRequired() {
    jobExecutionEntityCqlRepository.getFailedExecutionsWithoutCompletedDate().forEach(jobExecutionEntity -> {
      var jobExecution = jobExecutionEntity.getJobExecution();
      var completedDate = isNull(jobExecution.getLastUpdatedDate()) ?
        new Date() :
        jobExecution.getLastUpdatedDate();
      jobExecution.setCompletedDate(completedDate);
      jobExecutionEntity.setCompletedDate(completedDate);
      jobExecutionEntityCqlRepository.save(jobExecutionEntity);
    });
  }

  private void updateErrorLogIfJobIsExpired(UUID jobExecutionId) {
    var logEntities = errorLogEntityCqlRepository.getAllByJobExecutionId(jobExecutionId);
    if (!logEntities.isEmpty()) {
      var firstEntity = logEntities.get(0);
      var updatedLog = firstEntity.getErrorLog()
        .errorMessageCode(ERROR_JOB_IS_EXPIRED.getCode())
        .errorMessageValues(singletonList(ERROR_JOB_IS_EXPIRED.getDescription()));
      errorLogEntityCqlRepository.save(firstEntity.withErrorLog(updatedLog));
      // remove all the rest logs to have only 1 reason: job is expired
      logEntities.stream()
        .skip(1)
        .forEach(errorLogEntityCqlRepository::delete);
    }
  }
}
