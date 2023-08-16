package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobExecutionEntityRepository extends JpaRepository<JobExecutionEntity, UUID> {
}
