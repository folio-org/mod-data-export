package org.folio.dataexp.repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;

/** Repository for {@link JobExecutionEntity}. */
public interface JobExecutionEntityCqlRepository
    extends JpaCqlRepository<JobExecutionEntity, UUID> {

  /**
   * Gets expired jobs with status IN_PROGRESS.
   *
   * @param expirationDate expiration date
   * @return list of expired job execution entities
   */
  @Query(
      value =
          "SELECT * FROM job_executions WHERE jsonb ->> 'status' = 'IN_PROGRESS'"
              + " AND to_timestamp(cast(jsonb ->> 'lastUpdatedDate' AS BIGINT) / 1000) <= ?1",
      nativeQuery = true)
  List<JobExecutionEntity> getExpiredJobs(Date expirationDate);

  /**
   * Gets failed executions without completed date.
   *
   * @return list of failed job execution entities
   */
  @Query(
      value =
          "SELECT * FROM job_executions WHERE jsonb ->> 'status' = 'FAIL'"
              + " AND jsonb ->> 'completedDate' IS NULL",
      nativeQuery = true)
  List<JobExecutionEntity> getFailedExecutionsWithoutCompletedDate();

  /**
   * Gets all job executions by job profile ID.
   *
   * @param jobProfileId job profile UUID
   * @return list of job execution entities
   */
  @Query(value = "SELECT * FROM job_executions WHERE jobprofileid = ?1", nativeQuery = true)
  List<JobExecutionEntity> getAllByJobProfileId(UUID jobProfileId);
}
