package org.folio.dataexp.repository;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.domain.dto.JobExecution.StatusEnum.FAIL;
import static org.folio.dataexp.domain.dto.JobExecution.StatusEnum.IN_PROGRESS;

import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class JobExecutionEntityCqlRepositoryTest extends BaseDataExportInitializer {
  @Autowired
  JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;
  @Test
  void shouldGetExpiredJobs() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var expectedId = UUID.randomUUID();
      var expirationDate = new Date(new Date().getTime() - HOURS.toMillis(1));
      jobExecutionEntityCqlRepository.save(JobExecutionEntity.builder()
        .id(expectedId)
        .jobExecution(new JobExecution()
          .id(expectedId)
          .status(IN_PROGRESS)
          .lastUpdatedDate(new Date(new Date().getTime() - HOURS.toMillis(2))))
        .build());
      jobExecutionEntityCqlRepository.save(JobExecutionEntity.builder()
        .id(UUID.randomUUID())
        .jobExecution(new JobExecution()
          .id(UUID.randomUUID())
          .status(IN_PROGRESS)
          .lastUpdatedDate(new Date()))
        .build());

      var res = jobExecutionEntityCqlRepository.getExpiredJobs(expirationDate);

      assertThat(res).hasSize(1);
      var actualJobExecution = res.get(0).getJobExecution();
      assertThat(actualJobExecution.getId()).isEqualTo(expectedId);
    }
  }

  @Test
  void shouldGetFailedJobsWithoutCompletedDate() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var expectedId = UUID.randomUUID();
      jobExecutionEntityCqlRepository.save(JobExecutionEntity.builder()
        .id(expectedId)
        .jobExecution(new JobExecution()
          .id(expectedId)
          .status(FAIL))
        .build());
      jobExecutionEntityCqlRepository.save(JobExecutionEntity.builder()
        .id(UUID.randomUUID())
        .jobExecution(new JobExecution()
          .id(UUID.randomUUID())
          .status(IN_PROGRESS))
        .build());
      jobExecutionEntityCqlRepository.save(JobExecutionEntity.builder()
        .id(UUID.randomUUID())
        .jobExecution(new JobExecution()
          .id(UUID.randomUUID())
          .status(FAIL)
          .completedDate(new Date()))
        .build());

      var res = jobExecutionEntityCqlRepository.getFailedExecutionsWithoutCompletedDate();

      assertThat(res).hasSize(1);
      var actualJobExecution = res.get(0).getJobExecution();
      assertThat(actualJobExecution.getId()).isEqualTo(expectedId);
    }
  }
}
