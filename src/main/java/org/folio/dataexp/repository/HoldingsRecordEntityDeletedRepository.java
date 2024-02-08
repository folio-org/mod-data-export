package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.HoldingsRecordDeletedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface HoldingsRecordEntityDeletedRepository extends JpaRepository<HoldingsRecordDeletedEntity, UUID> {

  @Query(value = "SELECT id, jsonb FROM v_holdings_all_deleted where jsonb -> 'record' ->> 'discoverySuppress' is null OR jsonb -> 'record' ->> 'discoverySuppress' = 'false'", nativeQuery = true)
  List<HoldingsRecordDeletedEntity> findAllDeletedWhenSkipDiscoverySuppressed();
}
