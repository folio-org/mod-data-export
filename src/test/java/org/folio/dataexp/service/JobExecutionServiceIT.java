package org.folio.dataexp.service;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.domain.dto.JobExecution.StatusEnum.FAIL;
import static org.folio.dataexp.domain.dto.JobExecution.StatusEnum.IN_PROGRESS;
import static org.folio.dataexp.util.ErrorCode.ERROR_JOB_IS_EXPIRED;

import java.util.Date;
import java.util.UUID;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JobExecutionServiceTest extends BaseDataExportInitializer {
  @Autowired private JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;
  @Autowired private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  @Autowired private JobExecutionService jobExecutionService;

  @Test
  void shouldExpireJobExecutionsAndSetCompletedDateForFailedJobsIfNeeded() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var failedJobId = UUID.randomUUID();
      var expiredJobId = UUID.randomUUID();
      var lastUpdatedDate = new Date();
      var expiredDate = new Date(new Date().getTime() - HOURS.toMillis(1));

      jobExecutionEntityCqlRepository.save(
          JobExecutionEntity.builder()
              .id(failedJobId)
              .jobExecution(
                  new JobExecution().id(failedJobId).status(FAIL).lastUpdatedDate(lastUpdatedDate))
              .build());
      jobExecutionEntityCqlRepository.save(
          JobExecutionEntity.builder()
              .id(expiredJobId)
              .jobExecution(
                  new JobExecution()
                      .id(expiredJobId)
                      .status(IN_PROGRESS)
                      .lastUpdatedDate(expiredDate))
              .build());
      errorLogEntityCqlRepository.save(
          ErrorLogEntity.builder()
              .id(UUID.randomUUID())
              .errorLog(new ErrorLog().jobExecutionId(expiredJobId))
              .build());
      errorLogEntityCqlRepository.save(
          ErrorLogEntity.builder()
              .id(UUID.randomUUID())
              .errorLog(new ErrorLog().jobExecutionId(expiredJobId))
              .build());

      jobExecutionService.expireJobExecutions();

      var failedJob = jobExecutionEntityCqlRepository.findById(failedJobId);
      assertThat(failedJob).isPresent();
      assertThat(failedJob.get().getJobExecution().getId()).isEqualTo(failedJobId);
      assertThat(failedJob.get().getJobExecution().getCompletedDate()).isEqualTo(lastUpdatedDate);

      var expiredJob = jobExecutionEntityCqlRepository.findById(expiredJobId);
      assertThat(expiredJob).isPresent();
      var expiredJobExecution = expiredJob.get().getJobExecution();
      assertThat(expiredJobExecution.getStatus()).isEqualTo(FAIL);
      assertThat(expiredJobExecution.getCompletedDate()).isCloseTo(new Date(), SECONDS.toMillis(5));

      var errorLogEntities = errorLogEntityCqlRepository.getAllByJobExecutionId(expiredJobId);
      assertThat(errorLogEntities).hasSize(1);
      var errorLog = errorLogEntities.get(0).getErrorLog();
      assertThat(errorLog.getErrorMessageCode()).isEqualTo(ERROR_JOB_IS_EXPIRED.getCode());
      assertThat(errorLog.getErrorMessageValues())
          .isEqualTo(singletonList(ERROR_JOB_IS_EXPIRED.getDescription()));
    }
  }
}
