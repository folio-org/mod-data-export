package org.folio.dataexp.repository;

import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link JobExecutionExportFilesEntity}. */
public interface JobExecutionExportFilesEntityRepository
    extends JpaRepository<JobExecutionExportFilesEntity, UUID> {

  /**
   * Finds export files by job execution ID.
   *
   * @param jobExecutionId job execution UUID
   * @return list of job execution export files entities
   */
  List<JobExecutionExportFilesEntity> findByJobExecutionId(UUID jobExecutionId);
}
