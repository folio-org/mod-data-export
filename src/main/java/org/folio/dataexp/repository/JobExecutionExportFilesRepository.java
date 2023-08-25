package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobExecutionExportFilesRepository extends JpaRepository<JobExecutionExportFilesEntity, UUID> {
}
