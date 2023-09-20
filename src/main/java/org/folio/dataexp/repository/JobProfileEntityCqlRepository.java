package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.spring.cql.JpaCqlRepository;

import java.util.UUID;

public interface JobProfileEntityCqlRepository extends JpaCqlRepository<JobProfileEntity, UUID> {
}
