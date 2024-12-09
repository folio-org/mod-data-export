package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JobProfileEntityRepository extends JpaRepository<JobProfileEntity, UUID> {
}
