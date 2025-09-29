package org.folio.dataexp.repository;

import java.util.UUID;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link MappingProfileEntity}.
 */
public interface MappingProfileEntityRepository
    extends JpaRepository<MappingProfileEntity, UUID> {
}
