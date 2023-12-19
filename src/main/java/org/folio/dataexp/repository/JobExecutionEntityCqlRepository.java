package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface JobExecutionEntityCqlRepository extends JpaCqlRepository<JobExecutionEntity, UUID> {
  @Query(value = "SELECT * FROM job_executions WHERE jsonb ->> 'status' = 'IN_PROGRESS'" +
    " AND to_timestamp(cast(jsonb ->> 'lastUpdatedDate' AS BIGINT) / 1000) <= ?1", nativeQuery = true)
  List<JobExecutionEntity> getExpiredJobs(Date expirationDate);

  @Query(value = "SELECT * FROM job_executions WHERE jsonb ->> 'status' = 'FAIL'" +
    " AND jsonb ->> 'completedDate' IS NULL", nativeQuery = true)
  List<JobExecutionEntity> getFailedExecutionsWithoutCompletedDate();
}
