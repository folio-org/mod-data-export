package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.spring.cql.JpaCqlRepository;

import java.util.UUID;

public interface ErrorLogEntityCqlRepository extends JpaCqlRepository<ErrorLogEntity, UUID> {
}
