package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobProfileEntityRepository extends JpaRepository<JobProfileEntity, UUID> {
}
