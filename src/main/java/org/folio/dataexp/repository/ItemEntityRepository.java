package org.folio.dataexp.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.springframework.data.repository.Repository;

/** Repository for {@link ItemEntity}. */
public interface ItemEntityRepository extends Repository<ItemEntity, UUID> {

  /**
   * Finds items by holdings record ID.
   *
   * @param holdingsRecordId holdings record UUID
   * @return list of item entities
   */
  List<ItemEntity> findByHoldingsRecordIdIs(UUID holdingsRecordId);

  /**
   * Finds items by a set of holdings record IDs.
   *
   * @param ids set of holdings record UUIDs
   * @return list of item entities
   */
  List<ItemEntity> findByHoldingsRecordIdIn(Set<UUID> ids);
}
