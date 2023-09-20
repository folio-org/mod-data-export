package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.spring.cql.JpaCqlRepository;

import java.util.UUID;

public interface JobExecutionEntityCqlRepository extends JpaCqlRepository<JobExecutionEntity, UUID> {
}
