package org.folio.dataexp.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.InstanceWithHridEntity;
import org.springframework.data.repository.Repository;

/** Repository for {@link InstanceWithHridEntity}. */
public interface InstanceWithHridEntityRepository extends Repository<InstanceWithHridEntity, UUID> {

  /**
   * Finds instances with HRID by a set of IDs.
   *
   * @param ids set of UUIDs
   * @return list of instance with HRID entities
   */
  List<InstanceWithHridEntity> findByIdIn(Set<UUID> ids);
}
