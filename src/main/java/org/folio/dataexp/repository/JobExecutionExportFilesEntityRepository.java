package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobExecutionExportFilesEntityRepository extends JpaRepository<JobExecutionExportFilesEntity, UUID> {

  List<JobExecutionExportFilesEntity> findByJobExecutionId(UUID jobExecutionId);
}
