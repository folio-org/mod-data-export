package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ConfigurationEntity;
import org.folio.spring.cql.JpaCqlRepository;

public interface ConfigurationRepository extends JpaCqlRepository<ConfigurationEntity, String> {
}
