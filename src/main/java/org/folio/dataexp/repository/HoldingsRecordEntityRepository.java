package org.folio.dataexp.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link HoldingsRecordEntity}. */
public interface HoldingsRecordEntityRepository extends JpaRepository<HoldingsRecordEntity, UUID> {

  /**
   * Finds holdings by a set of IDs.
   *
   * @param ids set of UUIDs
   * @return list of holdings record entities
   */
  List<HoldingsRecordEntity> findByIdIn(Set<UUID> ids);

  /**
   * Finds holdings by instance ID.
   *
   * @param instanceId instance UUID
   * @return list of holdings record entities
   */
  List<HoldingsRecordEntity> findByInstanceIdIs(UUID instanceId);
}
