package org.folio.dataexp.repository;

import java.util.UUID;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repository for {@link JobExecutionEntity}. */
public interface JobExecutionEntityRepository extends JpaRepository<JobExecutionEntity, UUID> {

  /**
   * Gets next HRID value from sequence.
   *
   * @return next HRID value
   */
  @Query("SELECT nextval('job_execution_hrId')")
  int getHrid();
}
