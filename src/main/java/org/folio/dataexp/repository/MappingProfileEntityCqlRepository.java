package org.folio.dataexp.repository;

import java.util.UUID;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.spring.cql.JpaCqlRepository;

/** Repository for {@link MappingProfileEntity}. */
public interface MappingProfileEntityCqlRepository
    extends JpaCqlRepository<MappingProfileEntity, UUID> {}
