package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface JobExecutionEntityRepository extends JpaRepository<JobExecutionEntity, UUID> {

  @Query("SELECT nextval('job_execution_hrId')")
  int getHrid();
}
