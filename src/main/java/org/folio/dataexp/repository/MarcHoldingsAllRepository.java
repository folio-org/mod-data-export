package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface MarcHoldingsAllRepository extends Repository<MarcRecordEntity, UUID> {

  @Query(value = "SELECT * FROM v_marc_holdings_all_non_deleted WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<MarcRecordEntity> findMarcHoldingsAllNonDeleted(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT * FROM v_marc_holdings_all_non_deleted_non_suppressed WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<MarcRecordEntity> findMarcHoldingsAllNonDeletedNonSuppressed(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT * FROM v_holdings_all_marc_deleted ORDER BY id ASC", nativeQuery = true)
  List<MarcRecordEntity> findMarcHoldingsAllDeleted();

  @Query(value = "SELECT * FROM v_holdings_all_marc_deleted_not_suppressed ORDER BY id ASC", nativeQuery = true)
  List<MarcRecordEntity> findMarcHoldingsAllDeletedNonSuppressed();
}
