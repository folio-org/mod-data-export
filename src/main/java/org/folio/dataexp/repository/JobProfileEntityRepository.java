package org.folio.dataexp.repository;

import java.util.UUID;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link JobProfileEntity}. */
public interface JobProfileEntityRepository extends JpaRepository<JobProfileEntity, UUID> {}
