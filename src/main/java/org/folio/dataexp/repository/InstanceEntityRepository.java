package org.folio.dataexp.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link InstanceEntity}. */
public interface InstanceEntityRepository extends JpaRepository<InstanceEntity, UUID> {
  /**
   * Finds instances by a set of IDs.
   *
   * @param ids set of UUIDs
   * @return list of instance entities
   */
  List<InstanceEntity> findByIdIn(Set<UUID> ids);
}
