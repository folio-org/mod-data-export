package org.folio.dataexp.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.AuditInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link AuditInstanceEntity}.
 */
public interface AuditInstanceEntityRepository extends JpaRepository<AuditInstanceEntity, UUID> {
  /**
   * Finds audit instances by a set of IDs.
   *
   * @param ids set of UUIDs
   * @return list of audit instance entities
   */
  List<AuditInstanceEntity> findByIdIn(Set<UUID> ids);
}
