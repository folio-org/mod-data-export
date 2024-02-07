package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HoldingsRecordEntityRepository extends Repository<HoldingsRecordEntity, UUID> {

  List<HoldingsRecordEntity> findByIdIn(Set<UUID> ids);

  List<HoldingsRecordEntity> findByInstanceIdIs(UUID instanceId);

  Slice<HoldingsRecordEntity> findByIdGreaterThanEqualAndIdLessThanEqualAndIdNotInOrderByIdAsc(UUID fromId, UUID toId, Set<UUID> deletedMarcIds, Pageable page);

  Slice<HoldingsRecordEntity> findByIdGreaterThanEqualAndIdLessThanEqualOrderByIdAsc(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT * FROM v_holdings_all WHERE id BETWEEN ?1 AND ?2 AND (jsonb ->> 'discoverySuppress' is null OR jsonb ->> 'discoverySuppress' = 'false') ORDER BY id ASC", nativeQuery = true)
  Slice<HoldingsRecordEntity> findAllWhenSkipDiscoverySuppressed(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT * FROM v_holdings_all WHERE id BETWEEN ?1 AND ?2 AND (jsonb ->> 'discoverySuppress' is null OR jsonb ->> 'discoverySuppress' = 'false') AND id NOT IN ?3 ORDER BY id ASC", nativeQuery = true)
  Slice<HoldingsRecordEntity> findAllWhenSkipDiscoverySuppressedAndSkipDeletedMarc(UUID fromId, UUID toId, Set<UUID> deletedMarcIds, Pageable page);
}
